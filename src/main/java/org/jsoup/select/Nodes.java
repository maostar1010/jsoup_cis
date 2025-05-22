package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 A list of {@link Node} objects, with methods that act on every node in the list.
 <p>Methods that {@link #set(int, T) set}, {@link #remove(int) remove}, or {@link #replaceAll(UnaryOperator)
replace} Elements in the list will also act on the underlying {@link org.jsoup.nodes.Document DOM}.</p>

 @since 1.21.1
 */
public class Nodes<T extends Node> extends ArrayList<T> {
    // todo push applicable methods down from Elements to Nodes

    public Nodes() {
    }

    public Nodes(int initialCapacity) {
        super(initialCapacity);
    }

    public Nodes(Collection<T> nodes) {
        super(nodes);
    }

    public Nodes(List<T> nodes) {
        super(nodes);
    }

    @SafeVarargs
    public Nodes(T... nodes) {
        super(Arrays.asList(nodes));
    }

    /**
     * Creates a deep copy of these nodes.
     * @return a deep copy
     */
    @Override
    public Nodes<T> clone() {
        Nodes<T> clone = new Nodes<>(size());
        for (T node : this)
            clone.add((T) node.clone());
        return clone;
    }

    /**
     Convenience method to get the Nodes as a plain ArrayList. This allows modification to the list of nodes
     without modifying the source Document. I.e. whereas calling {@code nodes.remove(0)} will remove the nodes from
     both the Nodes and the DOM, {@code nodes.asList().remove(0)} will remove the node from the list only.
     <p>Each Node is still the same DOM connected Node.</p>

     @return a new ArrayList containing the nodes in this list
     @see #Nodes(List)
     */
    public ArrayList<T> asList() {
        return new ArrayList<>(this);
    }

    /**
     Remove each matched node from the DOM.
     <p>The nodes will still be retained in this list, in case further processing of them is desired.</p>
     <p>
     E.g. HTML: {@code <div><p>Hello</p> <p>there</p> <img></div>}<br>
     <code>doc.select("p").remove();</code><br>
     HTML = {@code <div> <img></div>}
     <p>
     Note that this method should not be used to clean user-submitted HTML; rather, use {@link org.jsoup.safety.Cleaner}
     to clean HTML.

     @return this, for chaining
     @see Element#empty()
     @see Elements#empty()
     @see #clear()
     */
    public Nodes<T> remove() {
        for (T node : this) {
            node.remove();
        }
        return this;
    }

    // ArrayList<T> methods that update the DOM:

    /**
     Replace the node at the specified index in this list, and in the DOM.

     @param index index of the node to replace
     @param node node to be stored at the specified position
     @return the old Node at this index
     */
    @Override
    public T set(int index, T node) {
        Validate.notNull(node);
        T old = super.set(index, node);
        old.replaceWith(node);
        return old;
    }

    /**
     Remove the node at the specified index in this list, and from the DOM.

     @param index the index of the node to be removed
     @return the old node at this index
     @see #deselect(int)
     */
    @Override
    public T remove(int index) {
        T old = super.remove(index);
        old.remove();
        return old;
    }

    /**
     Remove the specified node from this list, and from the DOM.

     @param o node to be removed from this list, if present
     @return if this list contained the Node
     @see #deselect(Object)
     */
    @Override
    public boolean remove(Object o) {
        int index = super.indexOf(o);
        if (index == -1) {
            return false;
        } else {
            remove(index);
            return true;
        }
    }

    /**
     Remove the node at the specified index in this list, but not from the DOM.

     @param index the index of the node to be removed
     @return the old node at this index
     @see #remove(int)
     */
    public T deselect(int index) {
        return super.remove(index);
    }

    /**
     Remove the specified node from this list, but not from the DOM.

     @param o node to be removed from this list, if present
     @return if this list contained the Node
     @see #remove(Object)
     */
    public boolean deselect(Object o) {
        return super.remove(o);
    }

    /**
     Removes all the nodes from this list, and each of them from the DOM.

     @see #deselectAll()
     */
    @Override
    public void clear() {
        remove();
        super.clear();
    }

    /**
     Like {@link #clear()}, removes all the nodes from this list, but not from the DOM.

     @see #clear()
     */
    public void deselectAll() {
        super.clear();
    }

    /**
     Removes from this list, and from the DOM, each of the nodes that are contained in the specified collection and are
     in this list.

     @param c collection containing nodes to be removed from this list
     @return {@code true} if nodes were removed from this list
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean anyRemoved = false;
        for (Object o : c) {
            anyRemoved |= this.remove(o);
        }
        return anyRemoved;
    }

    /**
     Retain in this list, and in the DOM, only the nodes that are in the specified collection and are in this list. In
     other words, remove nodes from this list and the DOM any item that is in this list but not in the specified
     collection.

     @param toRemove collection containing nodes to be retained in this list
     @return {@code true} if nodes were removed from this list
     @since 1.17.1
     */
    @Override
    public boolean retainAll(Collection<?> toRemove) {
        boolean anyRemoved = false;
        for (Iterator<T> it = this.iterator(); it.hasNext(); ) {
            T el = it.next();
            if (!toRemove.contains(el)) {
                it.remove();
                anyRemoved = true;
            }
        }
        return anyRemoved;
    }

    /**
     Remove from the list, and from the DOM, all nodes in this list that mach the given predicate.

     @param filter a predicate which returns {@code true} for nodes to be removed
     @return {@code true} if nodes were removed from this list
     */
    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        boolean anyRemoved = false;
        for (Iterator<T> it = this.iterator(); it.hasNext(); ) {
            T node = it.next();
            if (filter.test(node)) {
                it.remove();
                anyRemoved = true;
            }
        }
        return anyRemoved;
    }

    /**
     Replace each node in this list with the result of the operator, and update the DOM.

     @param operator the operator to apply to each node
     */
    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        for (int i = 0; i < this.size(); i++) {
            this.set(i, operator.apply(this.get(i)));
        }
    }
}
