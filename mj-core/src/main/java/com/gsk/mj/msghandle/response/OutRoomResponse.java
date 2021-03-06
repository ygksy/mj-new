package com.gsk.mj.msghandle.response;

import com.gsk.mj.net.constant.ConnectAPI;
import com.gsk.mj.net.model.ServerResponse;

import java.io.IOException;



public class OutRoomResponse extends ServerResponse {
	/**
	 * 
	 * @param status
	 * @param str( tpye:”0”//0退出房间    1解散房间
						   uuid:uuid//用户uuid
						   accountName:”名字”//退出房间玩家的名字(为空则表示是通知的自己)
						   status_code:”0”//”0”退出成功，”1” 退出失败
						   error：”消息” //退出失败时才显示消息
						   )
	 */
	public OutRoomResponse(int status,String str) {
		super(status, ConnectAPI.OUT_ROOM_RESPONSE);
		if(status>0){
			try {
				output.writeUTF(str);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
           	 output.close();
			}
		}
		//entireMsg();
	}

}
