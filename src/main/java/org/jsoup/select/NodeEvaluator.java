package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;

import java.util.Locale;

import static org.jsoup.internal.Normalizer.lowerCase;
import static org.jsoup.internal.StringUtil.normaliseWhitespace;

class NodeEvaluator extends Evaluator {
    final java.lang.Class<? extends LeafNode> type;
    final String selector;

    NodeEvaluator(java.lang.Class<? extends LeafNode> type, String selector) {
        super();
        this.type = type;
        this.selector = "::" + selector;
    }

    @Override
    protected int cost() {
        return 1;
    }

    @Override
    public boolean matches(Element root, Element element) {
        return false;
    }

    @Override boolean matches(Element root, LeafNode leaf) {
        return type.isInstance(leaf);
    }

    @Override boolean wantsNodes() {
        return true;
    }

    @Override
    public String toString() {
        return selector;
    }

    static class ContainsValue extends Evaluator {
        private final String searchText;

        public ContainsValue(String searchText) {
            this.searchText = lowerCase(normaliseWhitespace(searchText));
        }

        @Override
        public boolean matches(Element root, Element element) {
            return false;
        }

        @Override
        boolean matches(Element root, LeafNode leafNode) {
            return leafNode.nodeValue().toLowerCase(Locale.ROOT).contains(searchText);
        }

        @Override
        boolean wantsNodes() {
            return true;
        }

        @Override
        protected int cost() {
            return 10;
        }

        @Override
        public String toString() {
            return String.format(":contains(%s)", searchText);
        }
    }

}
