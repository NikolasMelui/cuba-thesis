package com.haulmont.cuba.core.sys.jpql.tree;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

/**
 * Author: Alexander Chevelev
 * Date: 30.10.2010
 * Time: 4:15:07
 */
public class AggregateExpressionNode extends BaseCustomNode {
    private AggregateExpressionNode(Token token) {
        super(token);
    }

    public AggregateExpressionNode(int type) {
        this(new CommonToken(type, ""));
    }

    @Override
    public String toString() {
        return "AGGREGATE EXPR";
    }

    @Override
    public Tree dupNode() {
        AggregateExpressionNode result = new AggregateExpressionNode(token);
        dupChildren(result);
        return result;
    }
}