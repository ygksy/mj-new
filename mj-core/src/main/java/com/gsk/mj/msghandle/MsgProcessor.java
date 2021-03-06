package com.gsk.mj.msghandle;

import com.gsk.mj.net.model.ClientRequest;
import com.gsk.mj.net.session.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public abstract class MsgProcessor {
	
	private static final Logger logger = LoggerFactory.getLogger(MsgProcessor.class);
	
	public void handle(GameSession gameSession, ClientRequest request){
		try {
			process(gameSession,request);
		} catch (Exception e) {
			logger.error("消息处理出错，msg code:"+request.getMsgCode());
			e.printStackTrace();
		}
	}
	
	public abstract void process(GameSession gameSession,ClientRequest request)throws Exception;
}
