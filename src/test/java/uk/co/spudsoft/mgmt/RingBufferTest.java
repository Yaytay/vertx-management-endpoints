/*
 * Copyright (C) 2023 njt
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class RingBufferTest {
  
  private static final Logger logger = LoggerFactory.getLogger(RingBufferTest.class);
  
  /**
   * Test of add method, of class RingBuffer.
   */
  @Test
  public void testAdd() {
    RingBuffer<String> rb = new RingBuffer<>(6);
    assertEquals(0, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[0], rb.toArray(i -> new String[i]));

    rb.add("One");
    assertEquals(1, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"One"}, rb.toArray(i -> new String[i]));

    rb.add("Two");
    assertEquals(2, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"One", "Two"}, rb.toArray(i -> new String[i]));

    rb.add("Three");
    assertEquals(3, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"One", "Two", "Three"}, rb.toArray(i -> new String[i]));

    rb.add("Four");
    assertEquals(4, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"One", "Two", "Three", "Four"}, rb.toArray(i -> new String[i]));

    rb.add("Five");
    assertEquals(5, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"One", "Two", "Three", "Four", "Five"}, rb.toArray(i -> new String[i]));

    rb.add("Six");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"One", "Two", "Three", "Four", "Five", "Six"}, rb.toArray(i -> new String[i]));

    rb.add("Seven");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"Two", "Three", "Four", "Five", "Six", "Seven"}, rb.toArray(i -> new String[i]));

    rb.add("Eight");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"Three", "Four", "Five", "Six", "Seven", "Eight"}, rb.toArray(i -> new String[i]));
    
    rb.add("Nine");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"Four", "Five", "Six", "Seven", "Eight", "Nine"}, rb.toArray(i -> new String[i]));
    
    rb.add("Ten");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"Five", "Six", "Seven", "Eight", "Nine", "Ten"}, rb.toArray(i -> new String[i]));
    
    rb.add("Eleven");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"Six", "Seven", "Eight", "Nine", "Ten", "Eleven"}, rb.toArray(i -> new String[i]));
    
    rb.add("Twelve");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve"}, rb.toArray(i -> new String[i]));
    
    rb.add("Thirteen");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen"}, rb.toArray(i -> new String[i]));
    
    rb.add("Fourteen");
    assertEquals(6, rb.size());
    logger.debug("Buffer: {}", (Object) rb.toArray(i -> new String[i]));
    assertArrayEquals(new String[]{"Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen"}, rb.toArray(i -> new String[i]));
    
  }  
}
