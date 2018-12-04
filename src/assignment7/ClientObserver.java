package assignment7;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Observable;
import java.util.Observer;

class ClientObserver extends PrintWriter implements Observer {

		public ClientObserver(OutputStream out) {
			super(out);
			}
		
		@Override
		public void update(Observable arg0, Object arg1) {
			this.println(arg1);
			this.flush();
		}		
	}