package com.haulmont.cuba.core.sys.jpql.tree;

import com.haulmont.cuba.core.sys.jpql.ErrorRec;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import java.util.List;

/**
 * Author: Alexander Chevelev
 * Date: 30.10.2010
 * Time: 4:15:07
 */
public class GroupByNode extends BaseCustomNode {
    private GroupByNode(Token token) {
        super(token);
    }

    public GroupByNode(int type) {
        this(new CommonToken(type, ""));
    }

    @Override
    public String toString() {
        return "GROUP BY";
    }

    @Override
    public Tree dupNode() {
        GroupByNode result = new GroupByNode(token);
        dupChildren(result);
        return result;
    }
}