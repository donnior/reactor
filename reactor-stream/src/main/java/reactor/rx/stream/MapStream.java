/*
 * Copyright (c) 2011-2015 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.rx.stream;


import reactor.fn.Function;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;
import reactor.rx.Stream;

import java.io.Serializable;
import java.util.Map;

/**
 * A SubscribableMap is an event-driven Map that signals logged operations to its subscribers. Useful for IO bound
 * map storage where acknowledgement is decoupled from writing operation (asynchronous). In that case, read operations
 * will often operate as local proxy cache read in a usual implementation, but it doesn't have to be enforced.
 * E.g. a Pivotal Gemfire™ implementation will delegate map operations to a region and will create listeners on subscribe.
 * A Chronicle implementation as provided in {@link reactor.rx.stream.io} will either allow write or just read off a chronicle backed persistent store.
 *
 * @author Stephane Maldini
 */
public abstract class MapStream<K,V> extends Stream<MapStream.Signal<K,V>> implements Map<K,V> {

	public enum Operation {
		put, putAll, remove, clear
	}

	public static class Signal<K, V> implements Serializable {
		Operation op;
		K         key;
		V         previous;
		V         value;

		Signal(Operation op, K key, V value, V previous) {
			this.op = op;
			this.key = key;
			this.value = value;
			this.previous = previous;
		}

		public Operation op() {
			return op;
		}

		public K key() {
			return key;
		}

		public V previous() {
			return previous;
		}

		public V value() {
			return value;
		}

		public Tuple2<K, V> pair() {
			return Tuple.of(key, value);
		}

		@Override
		public String toString() {
			return "SubscribableMap.Signal{" +
					"op=" + op +
					(key != null ? ", key=" + key : "") +
					(previous != null ? ", previous=" + previous : "") +
					(value != null ? ", value=" + value : "") +
					'}';
		}

		public static <K, V> Signal<K, V> create(Operation op) {
			return new Signal<>(op, null, null, null);
		}

		public static <K, V> Signal<K, V> create(Operation op, K key) {
			return new Signal<>(op, key, null, null);
		}

		public static <K, V> Signal<K, V> create(Operation op, K key, V value) {
			return new Signal<>(op, key, value, null);
		}

		public static <K, V> Signal<K, V> create(Operation op, K key, V value, V previous) {
			return new Signal<>(op, key, value, previous);
		}
	}

	public static class MutableSignal<K,V> extends Signal<K,V>{
		public MutableSignal() {
			super(null, null, null, null);
		}

		public void op(Operation op) {
			this.op = op;
		}

		public void key(K key) {
			this.key = key;
		}

		public void previous(V previous) {
			this.previous = previous;
		}

		public void value(V value) {
			this.value = value;
		}
	}

	/**
	 * Return a Stream of key/value tuples for only new or updated entries.
	 *
	 * @return new Stream
	 */
	public Stream<Tuple2<K,V>> onPut(){
		return map(new Function<Signal<K, V>, Tuple2<K, V>>() {
			@Override
			public Tuple2<K, V> apply(Signal<K, V> kvSignal) {
				if(kvSignal.op == Operation.put){
					return kvSignal.pair();
				}else{
					return null;
				}
			}
		});
	}

	/**
	 * Return a Stream of key/value tuples for only removed entries.
	 *
	 * @return new Stream
	 */
	public Stream<K> onRemove(){
		return map(new Function<Signal<K, V>, K>() {
			@Override
			public K apply(Signal<K, V> kvSignal) {
				if(kvSignal.op == Operation.remove){
					return kvSignal.key;
				}else{
					return null;
				}
			}
		});
	}
}
