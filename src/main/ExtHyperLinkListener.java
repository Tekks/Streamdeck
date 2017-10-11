package main;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

class ExtHyperLinkListener implements HyperlinkListener {
	JEditorPane editorPane;

	public ExtHyperLinkListener(JEditorPane editorPane) {
		this.editorPane = editorPane;
	}

	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
			try {
				Desktop.getDesktop().browse(new URI(e.getURL().toString()));
			} catch (IOException | URISyntaxException err) {
				err.printStackTrace();
			}
	}
}