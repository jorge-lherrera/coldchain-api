/**
 * Every listener on a broker topic, grouped by module. A listener reads a message and calls the module's
 * published contract, exactly as a controller does with a request; it belongs to the delivery adapter
 * and never to the module it serves.
 */
package com.coldchain.delivery.messaging;
