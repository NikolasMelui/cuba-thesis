package com.haulmont.cuba.core.sys.jpql.transform;

import com.haulmont.cuba.core.sys.jpql.QueryTreeAnalyzer;
import com.haulmont.cuba.core.sys.jpql.antlr.JPALexer;
import com.haulmont.cuba.core.sys.jpql.tree.*;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Alexander Chevelev
 * Date: 26.03.2011
 * Time: 1:56:20
 */
public class QueryTreeTransformer extends QueryTreeAnalyzer {

    public QueryTreeTransformer() {
    }


    public void mixinWhereConditionsIntoTree(CommonTree whereTreeToMixIn) {
        CommonTree whereTreeToMixWithin = (CommonTree) tree.getFirstChildWithType(JPALexer.T_CONDITION);
        if (whereTreeToMixWithin == null) {
            FromNode fromNode = (FromNode) tree.getFirstChildWithType(JPALexer.T_SOURCES);
            List<Tree> endingNodes = new ArrayList<Tree>();
            int deletionCount = tree.getChildCount() - (fromNode.getChildIndex() + 1);
            for (int i = 0; i < deletionCount; i++) {
                endingNodes.add((Tree) tree.deleteChild(fromNode.getChildIndex() + 1));
            }
            tree.addChild(whereTreeToMixIn);
            for (Tree endingNode : endingNodes) {
                tree.addChild(endingNode);
            }
            tree.freshenParentAndChildIndexes();
        } else {
            CommonTree and = new CommonTree(new CommonToken(JPALexer.AND, "and"));
            whereTreeToMixWithin.addChild(and);
            for (Object o : whereTreeToMixIn.getChildren()) {
                CommonTree t = (CommonTree) o;
                whereTreeToMixWithin.addChild(t.dupNode());
            }
            whereTreeToMixIn.freshenParentAndChildIndexes();
        }
    }

    public void mixinJoinIntoTree(CommonTree joinClause, EntityReference entityRef, boolean renameVariable) {
        CommonTree from = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SOURCES);
        for (int i = 0; i < from.getChildCount(); i++) {
            SelectionSourceNode sourceNode = (SelectionSourceNode) from.getChild(i);
            if (sourceNode.getChild(0) instanceof IdentificationVariableNode) {
                IdentificationVariableNode entityVariableNode = (IdentificationVariableNode) sourceNode.getChild(0);
                if (entityRef.isJoinableTo(entityVariableNode)) {
                    String variableName = entityVariableNode.getVariableName();
                    JoinVariableNode joinVariableNode = (JoinVariableNode) joinClause;
                    if (renameVariable) {
                        PathNode path = joinVariableNode.getPathNode();
                        path.renameVariableTo(variableName);
                    }
                    sourceNode.addChild(joinClause);
                    from.freshenParentAndChildIndexes();
                    return;
                }
            }
        }
        throw new RuntimeException("Join mixing failed. Cannot find selected entity with name " + entityRef);
    }

    public void addSelectionSource(CommonTree selectionSource) {
        CommonTree from = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SOURCES);
        from.addChild(selectionSource);
        from.freshenParentAndChildIndexes();
    }

    public void replaceWithCount(EntityReference entityRef) {
        Tree selectedItems = tree.getFirstChildWithType(JPALexer.T_SELECTED_ITEMS);
        boolean isDistinct = "DISTINCT".equalsIgnoreCase(selectedItems.getChild(0).getText());
        if (!(isDistinct && selectedItems.getChildCount() == 2 ||
                selectedItems.getChildCount() == 1))
            throw new IllegalStateException("Cannot replace with count if multiple fields selected");

        SelectedItemNode selectedItemNode;
        if (isDistinct)
            selectedItems.deleteChild(0);

        selectedItemNode = (SelectedItemNode) selectedItems.getChild(0);
        AggregateExpressionNode countNode = createCountNode(entityRef, isDistinct);
        selectedItemNode.deleteChild(0);
        selectedItemNode.addChild(countNode);

        Tree orderBy = tree.getFirstChildWithType(JPALexer.T_ORDER_BY);
        if (orderBy != null) {
            tree.deleteChild(orderBy.getChildIndex());
        }
        tree.freshenParentAndChildIndexes();
    }

    public void replaceOrderBy(PathEntityReference orderingFieldRef, boolean desc) {
        CommonTree orderBy = (CommonTree) tree.getFirstChildWithType(JPALexer.T_ORDER_BY);
        OrderByFieldNode orderByField;
        if (orderBy == null) {
            orderByField = new OrderByFieldNode(JPALexer.T_ORDER_BY_FIELD);

            orderBy = new OrderByNode(JPALexer.T_ORDER_BY);
            orderBy.addChild(new CommonTree(new CommonToken(JPALexer.ORDER, "order")));
            orderBy.addChild(new CommonTree(new CommonToken(JPALexer.BY, "by")));
            orderBy.addChild(orderByField);
            tree.addChild(orderBy);
        } else {
            orderByField = (OrderByFieldNode) orderBy.getFirstChildWithType(JPALexer.T_ORDER_BY_FIELD);
            if (orderByField == null)
                throw new IllegalStateException("No ordering field found");

            if (!(orderByField.getChildCount() == 1 || orderByField.getChildCount() == 2)) {
                throw new IllegalStateException("Invalid order by field node children count: " + orderByField.getChildCount());
            }

            orderByField.deleteChild(0);
        }

        PathNode pathNode = orderingFieldRef.getPathNode();
        if (pathNode.getChildCount() > 1) {
            CommonTree lastNode = (CommonTree) pathNode.deleteChild(pathNode.getChildCount() - 1);
            String variableName = pathNode.asPathString('_');

            PathNode orderingNode = new PathNode(JPALexer.T_SELECTED_FIELD, variableName);
            orderingNode.addDefaultChild(lastNode.getText());
            orderByField.addChild(orderingNode);

            JoinVariableNode joinNode = new JoinVariableNode(JPALexer.T_JOIN_VAR, "left join", variableName);
            joinNode.addChild(pathNode);

            CommonTree from = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SOURCES);
            from.getChild(0).addChild(joinNode); // assumption
        } else {
            orderByField.addChild(orderingFieldRef.createNode());
        }

        if (desc) {
            orderByField.addChild(new CommonTree(new CommonToken(JPALexer.DESC, "DESC")));
        }
        orderByField.freshenParentAndChildIndexes();
    }

    private AggregateExpressionNode createCountNode(EntityReference ref, boolean distinct) {
        AggregateExpressionNode result = new AggregateExpressionNode(JPALexer.T_AGGREGATE_EXPR);

        result.addChild(new CommonTree(new CommonToken(JPALexer.COUNT, "COUNT")));
        result.addChild(new CommonTree(new CommonToken(JPALexer.LPAREN, "(")));
        if (distinct) {
            result.addChild(new CommonTree(new CommonToken(JPALexer.DISTINCT, "DISTINCT")));
        }
        result.addChild(ref.createNode());
        result.addChild(new CommonTree(new CommonToken(JPALexer.RPAREN, ")")));
        return result;
    }

}

