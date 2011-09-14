/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.autocomplete.impl;

import com.haulmont.cuba.core.sys.jpql.DomainModel;
import com.haulmont.cuba.core.sys.jpql.Parser;
import com.haulmont.cuba.core.sys.jpql.antlr.JPALexer;
import com.haulmont.cuba.core.sys.jpql.model.Entity;
import com.haulmont.cuba.core.sys.jpql.model.EntityBuilder;
import com.haulmont.cuba.core.sys.jpql.transform.PathEntityReference;
import com.haulmont.cuba.core.sys.jpql.transform.QueryTreeTransformer;
import com.haulmont.cuba.core.sys.jpql.transform.VariableEntityReference;
import com.haulmont.cuba.core.sys.jpql.tree.*;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Author: Alexander Chevelev
 * Date: 01.04.2011
 * Time: 21:06:39
 */
public class QueryAnalyzerTest {
    @Test
    public void testTree() throws RecognitionException {
        DomainModel model = prepareDomainModel();

        QueryTreeTransformer qa = new QueryTreeTransformer();
        qa.prepare(model, "select f from sec$SearchFolder f " +
                "left join f.user u " +
                "left join f.presentation p " +
                "where (f.user.id = ?1 or f.user is null) " +
                "order by f.sortOrder, f.name");
        System.out.println("");
    }

    @Test
    public void mixinJoinIntoTree() throws RecognitionException {
        DomainModel model = prepareDomainModel();

        QueryTreeTransformer qa = new QueryTreeTransformer();
        qa.prepare(model, "select c from Car c");

        CommonTree tree = qa.getTree();
        CommonTree sources = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SOURCES);
        assertEquals(1, sources.getChildCount());
        assertTrue(sources.getChild(0) instanceof SelectionSourceNode);
        CommonTree source = (CommonTree) sources.getFirstChildWithType(JPALexer.T_SOURCE);
        assertTrue(source.getChild(0) instanceof IdentificationVariableNode);

        JoinVariableNode join = (JoinVariableNode) Parser.parseJoinClause("join a.drivers d");
        qa.mixinJoinIntoTree(join, new VariableEntityReference("Car", "c"), true);

        tree = qa.getTree();
        sources = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SOURCES);
        assertEquals(1, sources.getChildCount());
        SelectionSourceNode sourceNode = (SelectionSourceNode) sources.getChild(0);
        assertEquals(2, sourceNode.getChildCount());
        assertTrue(sourceNode.getChild(0) instanceof IdentificationVariableNode);
        assertTrue(sourceNode.getChild(1) instanceof JoinVariableNode);
        JoinVariableNode joinNode = (JoinVariableNode) sourceNode.getChild(1);
        assertEquals("d", joinNode.getVariableName());
        assertEquals("c", ((PathNode) join.getChild(0)).getEntityVariableName());
    }

    @Test
    public void mixinJoinIntoTree_with_in_collections() throws RecognitionException {
        EntityBuilder builder = new EntityBuilder();

        builder.startNewEntity("HomeBase");
        builder.addStringAttribute("name");
        Entity homeBase = builder.produce();

        builder.startNewEntity("Driver");
        builder.addStringAttribute("name");
        builder.addStringAttribute("signal");
        Entity driver = builder.produce();

        builder.startNewEntity("Car");
        builder.addStringAttribute("model");
        builder.addCollectionReferenceAttribute("drivers", "Driver");
        builder.addReferenceAttribute("station", "HomeBase");
        Entity car = builder.produce();
        DomainModel model = new DomainModel(car, driver, homeBase);

        JoinVariableNode join = (JoinVariableNode) Parser.parseJoinClause("join c.station h");

        QueryTreeTransformer qa = new QueryTreeTransformer();
        qa.prepare(model, "select d.name from Car c, in(c.drivers) d");

        CommonTree tree = qa.getTree();
        CommonTree sources = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SOURCES);
        assertEquals(2, sources.getChildCount());

        assertTrue(sources.getChild(0) instanceof SelectionSourceNode);
        CommonTree source0 = (CommonTree) sources.getChild(0);
        assertEquals(1, source0.getChildCount());
        assertTrue(source0.getChild(0) instanceof IdentificationVariableNode);

        assertTrue(sources.getChild(1) instanceof SelectionSourceNode);
        CommonTree source1 = (CommonTree) sources.getChild(1);
        assertTrue(source1.getChild(0) instanceof CollectionMemberNode);

        qa.mixinJoinIntoTree(join, new VariableEntityReference("Car", "c"), true);

        tree = qa.getTree();
        sources = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SOURCES);
        assertEquals(2, sources.getChildCount());
        assertTrue(sources.getChild(0) instanceof SelectionSourceNode);
        source0 = (CommonTree) sources.getChild(0);
        assertEquals(2, source0.getChildCount());
        assertTrue(source0.getChild(0) instanceof IdentificationVariableNode);
        assertTrue(source0.getChild(1) instanceof JoinVariableNode);

        assertTrue(sources.getChild(1) instanceof SelectionSourceNode);
        source1 = (CommonTree) sources.getChild(1);
        assertTrue(source1.getChild(0) instanceof CollectionMemberNode);
    }


    @Test
    public void mixinWhereConditionsIntoTree() throws RecognitionException {
        DomainModel model = prepareDomainModel();

        QueryTreeTransformer qa = new QueryTreeTransformer();
        qa.prepare(model, "select c from Car c");

        WhereNode where = (WhereNode) Parser.parseWhereClause("where c.model = ?1");

        CommonTree tree = qa.getTree();
        assertNull(tree.getFirstChildWithType(JPALexer.T_CONDITION));

        qa.mixinWhereConditionsIntoTree(where);

        tree = qa.getTree();
        assertNotNull(tree.getFirstChildWithType(JPALexer.T_CONDITION));
    }

    @Test
    public void replaceOrderBy() throws RecognitionException {
        DomainModel model = prepareDomainModel();

        QueryTreeTransformer qa = new QueryTreeTransformer();
        qa.prepare(model, "select c from Car c order by c.model");

        CommonTree tree = qa.getTree();
        CommonTree orderByNode = (CommonTree) tree.getFirstChildWithType(JPALexer.T_ORDER_BY);
        Tree orderByField = orderByNode.getFirstChildWithType(JPALexer.T_ORDER_BY_FIELD);
        assertEquals(1, orderByField.getChildCount());
        PathNode pathNode = (PathNode) orderByField.getChild(0);
        assertEquals("c", pathNode.getEntityVariableName());
        assertEquals("model", pathNode.getChild(0).getText());

        pathNode = new PathNode(JPALexer.T_SELECTED_FIELD, "c");
        pathNode.addDefaultChild("regNumber");
        qa.replaceOrderBy(new PathEntityReference(pathNode, "Car"), true);

        assertEquals(2, orderByField.getChildCount());
        pathNode = (PathNode) orderByField.getChild(0);
        assertEquals("c", pathNode.getEntityVariableName());
        assertEquals("regNumber", pathNode.getChild(0).getText());
        assertEquals("DESC", orderByField.getChild(1).getText());
    }

    @Test
    public void replaceWithCount() throws RecognitionException {
        DomainModel model = prepareDomainModel();

        QueryTreeTransformer qa = new QueryTreeTransformer();
        qa.prepare(model, "select c from Car c order by c.model");

        CommonTree tree = qa.getTree();
        CommonTree selectedItems = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SELECTED_ITEMS);
        Tree selectedItem = selectedItems.getFirstChildWithType(JPALexer.T_SELECTED_ITEM);
        PathNode pathNode = (PathNode) selectedItem.getChild(0);
        assertEquals("c", pathNode.getEntityVariableName());
        assertEquals(0, pathNode.getChildCount());
        CommonTree orderByNode = (CommonTree) tree.getFirstChildWithType(JPALexer.T_ORDER_BY);

        assertNotNull(orderByNode.getFirstChildWithType(JPALexer.T_ORDER_BY_FIELD));

        qa.replaceWithCount(new VariableEntityReference("Car", "c"));

        assertTrue(selectedItem.getChild(0) instanceof AggregateExpressionNode);
        AggregateExpressionNode countExpr = (AggregateExpressionNode) selectedItem.getChild(0);

        assertEquals("COUNT", countExpr.getChild(0).getText());
        assertEquals("c", countExpr.getChild(2).getText());
        assertEquals(4, countExpr.getChildCount());
        assertNull(orderByNode.getFirstChildWithType(JPALexer.T_ORDER_BY));
    }

    @Test
    public void replaceWithCount_distinct() throws RecognitionException {
        DomainModel model = prepareDomainModel();

        QueryTreeTransformer qa = new QueryTreeTransformer();
        qa.prepare(model, "select distinct d from Car c, in(c.drivers) d order by d.name");

        CommonTree tree = qa.getTree();
        CommonTree selectedItems = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SELECTED_ITEMS);
        Tree selectedItem = selectedItems.getFirstChildWithType(JPALexer.T_SELECTED_ITEM);
        PathNode pathNode = (PathNode) selectedItem.getChild(0);
        assertEquals("d", pathNode.getEntityVariableName());
        assertEquals(0, pathNode.getChildCount());
        CommonTree orderByNode = (CommonTree) tree.getFirstChildWithType(JPALexer.T_ORDER_BY);

        assertNotNull(orderByNode.getFirstChildWithType(JPALexer.T_ORDER_BY_FIELD));

        qa.replaceWithCount(new VariableEntityReference("Driver", "d"));

        selectedItems = (CommonTree) tree.getFirstChildWithType(JPALexer.T_SELECTED_ITEMS);
        assertEquals(1, selectedItems.getChildCount());
        selectedItem = selectedItems.getFirstChildWithType(JPALexer.T_SELECTED_ITEM);
        assertTrue(selectedItem.getChild(0) instanceof AggregateExpressionNode);
        AggregateExpressionNode countExpr = (AggregateExpressionNode) selectedItem.getChild(0);

        assertEquals("COUNT", countExpr.getChild(0).getText());
        assertEquals("DISTINCT", countExpr.getChild(2).getText());
        assertEquals("d", countExpr.getChild(3).getText());
        assertEquals(5, countExpr.getChildCount());
        assertNull(orderByNode.getFirstChildWithType(JPALexer.T_ORDER_BY));
    }

    private DomainModel prepareDomainModel() {
        EntityBuilder builder = new EntityBuilder();
        Entity driver = builder.produceImmediately("Driver", "name", "signal");

        builder.startNewEntity("Car");
        builder.addStringAttribute("model");
        builder.addStringAttribute("regNumber");
        builder.addCollectionReferenceAttribute("drivers", "Driver");
        Entity car = builder.produce();
        return new DomainModel(car, driver);
    }
}
