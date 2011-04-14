package com.haulmont.cuba.core.sys.jpql.tree;

import com.haulmont.cuba.core.sys.jpql.ErrorRec;
import com.haulmont.cuba.core.sys.jpql.QueryBuilder;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import java.util.List;

/**
 * Author: Alexander Chevelev
 * Date: 01.04.2011
 * Time: 20:22:32
 */
public class JoinVariableNode extends BaseJoinNode {
    private String joinSpec;

    public JoinVariableNode(Token token, String joinSpec, String variableName) {
        super(token, variableName);
        this.joinSpec = joinSpec;
    }

    public JoinVariableNode(int type, String joinSpec, String variableName) {
        this(new CommonToken(type, ""), joinSpec, variableName);
    }

    @Override
    public String toString() {
        return (token != null ? token.getText() : "") + "Join variable: " + variableName;
    }

    @Override
    public Tree dupNode() {
        JoinVariableNode result = new JoinVariableNode(token, joinSpec, variableName);
        dupChildren(result);
        return result;
    }

    public CommonTree treeToQueryPre(QueryBuilder sb, List<ErrorRec> invalidNodes) {
        sb.appendSpace();
        sb.appendString(joinSpec);
        sb.appendSpace();
        return this;
    }

    public CommonTree treeToQueryPost(QueryBuilder sb, List<ErrorRec> invalidNodes) {
        // должно появится после определения сущности, из которой выбирают, поэтому в post
        sb.appendSpace();
        sb.appendString(variableName);
        return this;
    }

    public PathNode getPathNode() {
        return (PathNode) getChild(0);
    }
}
