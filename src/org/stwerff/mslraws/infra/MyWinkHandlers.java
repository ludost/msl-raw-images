package org.stwerff.mslraws.infra;

import java.util.Arrays;
import java.util.List;

import org.apache.wink.server.handlers.HandlersFactory;

public class MyWinkHandlers extends HandlersFactory {

	public MyWinkHandlers() {
	}
	public List<? extends org.apache.wink.server.handlers.ResponseHandler> getResponseHandlers() {
	    return Arrays.asList(new org.stwerff.mslraws.infra.FlushDB());
	}
}
