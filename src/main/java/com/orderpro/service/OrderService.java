package com.orderpro.service;

/**
 * Consumer slice contract, ported from main.pc.
 * Methods return 0 on success (matching the C return codes).
 */
public interface OrderService {

  /**
   * Ports Order() — lists shops and menus, then inserts one orders row and
   * its order_lines rows in a single transaction (completing the
   * `// TODO: 주문 트랜잭션 처리` left in the C code).
   */
  int order();

  /**
   * Ports ReadOrdersFromConsumer() — order history join (orders,
   * order_lines, menus, shops, users) plus per-order line detail.
   */
  int readOrdersFromConsumer();
}
