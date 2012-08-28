package org.stwerff.mslraws.infra;

import java.util.Properties;

import org.apache.wink.server.handlers.HandlersChain;
import org.apache.wink.server.handlers.MessageContext;
import org.apache.wink.server.handlers.ResponseHandler;

import com.chap.memo.memoNodes.MemoNode;

public class FlushDB implements ResponseHandler {
	@Override
	public void init(Properties arg0) {
	}

	@Override
	public void handleResponse(MessageContext context, HandlersChain chain)
			throws Throwable {
		MemoNode.flushDB();
		chain.doChain(context);
	}

}
