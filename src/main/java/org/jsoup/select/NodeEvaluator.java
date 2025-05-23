package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;

import java.util.Locale;
import java.util.regex.Pattern;

import static org.jsoup.internal.Normalizer.lowerCase;
import static org.jsoup.internal.StringUtil.normaliseWhitespace;

class NodeEvaluator extends Evaluator {
    final java.lang.Class<? extends Node> type;
    final String selector;

    NodeEvaluator(java.lang.Class<? extends Node> type, String selector) {
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
        return evaluateMatch(element);
    }

    @Override boolean matches(Element root, LeafNode leaf) {
        return evaluateMatch(leaf);
    }

    boolean evaluateMatch(Node node) {
        return type.isInstance(node);
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
            return evaluateMatch(element);
        }

        @Override
        boolean matches(Element root, LeafNode leafNode) {
            return evaluateMatch(leafNode);
        }

        boolean evaluateMatch(Node node) {
            return node.nodeValue().toLowerCase(Locale.ROOT).contains(searchText);
        }

        @Override
        boolean wantsNodes() {
            return true;
        }

        @Override
        protected int cost() {
            return 6;
        }

        @Override
        public String toString() {
            return String.format(":contains(%s)", searchText);
        }
    }

    /**
     Matches nodes with no value or only whitespace.
     */
    static class BlankValue extends Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            return evaluateMatch(element);
        }

        @Override
        boolean matches(Element root, LeafNode leafNode) {
            return evaluateMatch(leafNode);
        }

        static boolean evaluateMatch(Node node) {
            return StringUtil.isBlank(node.nodeValue());
        }

        @Override
        protected int cost() {
            return 4;
        }

        @Override
        public String toString() {
            return ":blank";
        }

        @Override
        boolean wantsNodes() {
            return true;
        }
    }

    static class MatchesValue extends Evaluator {
        private final Pattern pattern;

        protected MatchesValue(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return evaluateMatch(element);
        }

        @Override
        boolean matches(Element root, LeafNode leafNode) {
            return evaluateMatch(leafNode);
        }

        boolean evaluateMatch(Node node) {
            return pattern.matcher(node.nodeValue()).find();
        }

        @Override
        protected int cost() {
            return 8;
        }

        @Override
        public String toString() {
            return String.format(":matches(%s)", pattern);
        }

        @Override
        boolean wantsNodes() {
            return true;
        }
    }
}
