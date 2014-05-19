/**
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS
 * OF TITLE, FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under
 * the License.
 */
package com.yammer.collections.azure;

import com.google.common.base.Optional;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

class SetView<E> extends AbstractSet<E> {
    private final Collection<E> collectionView;

    private SetView(Collection<E> collectionView) {
        this.collectionView = collectionView;
    }

    static <E> SetView<E> fromSetCollectionView(Collection<E> collection) {
        return new SetView<>(collection);
    }

    static <E> SetView<E> fromCollectionView(Collection<E> collection) {
        return new NonSetCollectionBasedSetView<>(collection);
    }

    @Override
    public int size() {
        return collectionView.size();
    }

    @Override
    public boolean isEmpty() {
        return collectionView.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return collectionView.contains(o);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<E> iterator() {
        return collectionView.iterator();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean containsAll(Collection<?> c) {
        return collectionView.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private static class NonSetCollectionBasedSetView<E> extends SetView<E> {
        private final Collection<E> collectionView;

        public NonSetCollectionBasedSetView(Collection<E> collectionView) {
            super(collectionView);
            this.collectionView = collectionView;
        }

        @Override
        public int size() {
            int size = 0;
            for (E ignored : this) {
                size++;
            }
            return size;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<E> iterator() {
            return new UniequeIterator(collectionView.iterator());
        }
    }

    // this iterator has memory impact (maintains the occurences set) but allows for not loading the full set into memory immidiately
    private static class UniequeIterator<E> implements Iterator<E> {
        private final Iterator<E> baseIterator;
        private final Set<E> occurences;
        private Optional<E> next;

        private UniequeIterator(Iterator<E> baseIterator) {
            this.baseIterator = baseIterator;
            occurences = new HashSet<>();
            next = Optional.absent();
        }

        @Override
        public boolean hasNext() {
            if (!next.isPresent()) {
                next = internalNext();
            }
            return next.isPresent();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            E nextElem = next.get();
            next = Optional.absent();
            return nextElem;
        }

        private Optional<E> internalNext() {
            E candiateNext;
            do {
                if (!baseIterator.hasNext()) {
                    return Optional.absent();
                }
                candiateNext = baseIterator.next();
            } while (!occurences.add(candiateNext));

            return Optional.of(candiateNext);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
