/*
 * Copyright (C) 2023 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.mgmt;

import java.util.function.IntFunction;

/**
 *  A FIFO queue of bounded size that automatically removes the oldest item when adding a new one would exceed the capacity.
 * 
 *
 * @param <T> The type of object stored in the RingBuffer.
 * @author jtalbut
 */
public class RingBuffer<T> {
  
  private final int capacity;
  private final Object[] array;
  private int insert = 0;
  private int size = 0;

  /**
   * Constructor.
   * 
   * @param capacity The size of the rung buffer.
   */
  public RingBuffer(int capacity) {
    this.capacity = capacity;
    this.array = new Object[capacity + 1];
  }
  
  /**
   * Add an item to the ring buffer.
   * @param item The item to add to the ring buffer.
   */
  public void add(T item) {
    synchronized (array) {
      array[insert] = item;
      ++insert;
      if (insert == capacity + 1) {
        insert = 0;
      }
      array[insert] = null;
      if (size < capacity) {
        ++size;
      }
    }
  }

  /**
   * Get the size of the ring buffer.
   * @return the size of the ring buffer.
   */
  public int size() {
    return size;
  }
  
  /**
   * Copy the contents of the RingBuffer into a newly allocated array.
   * @param generator Generator for the allocation of the array.
   * @return the contents of the RingBuffer in a newly allocated array.
   */
  @SuppressWarnings("unchecked")
  public T[] toArray(IntFunction<T[]> generator) {   
    T[] result = generator.apply(size);
    synchronized (array) {
      int start = insert + 1;
      if (start == size + 1) {
        start = 0;
      }

      for (int i = start, j = 0; array[i] != null; ++i, ++j) {
        result[j] = (T) array[i];
        if (i == capacity) {
          i = -1;
        }
      }
        
      return result;
    }
  }
}
