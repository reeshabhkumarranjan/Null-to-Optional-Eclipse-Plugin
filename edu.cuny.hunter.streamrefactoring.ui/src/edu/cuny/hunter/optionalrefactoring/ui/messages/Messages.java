/**
 * 
 */
package edu.cuny.hunter.optionalrefactoring.ui.messages;

import org.eclipse.osgi.util.NLS;

/**
 * @author raffi
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "edu.cuny.hunter.optionalrefactoring.ui.messages.messages"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		super();
	}
}
