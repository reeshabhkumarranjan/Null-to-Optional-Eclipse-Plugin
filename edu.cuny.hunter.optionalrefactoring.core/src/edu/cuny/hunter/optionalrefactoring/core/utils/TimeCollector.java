/**
 *
 */
package edu.cuny.hunter.optionalrefactoring.core.utils;

/**
 * @author raffi
 *
 */
public class TimeCollector {

	private long collectedTime;
	private long start;

	public void clear() {
		this.collectedTime = 0;
	}

	public long getCollectedTime() {
		return this.collectedTime;
	}

	public void start() {
		this.start = System.currentTimeMillis();
	}

	public void stop() {
		final long elapsed = System.currentTimeMillis() - this.start;
		this.collectedTime += elapsed;
	}
}
