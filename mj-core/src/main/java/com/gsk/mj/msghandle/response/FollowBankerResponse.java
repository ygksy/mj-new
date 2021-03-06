package com.gsk.mj.msghandle.response;



import com.gsk.mj.net.constant.ConnectAPI;
import com.gsk.mj.net.model.ServerResponse;

import java.io.IOException;

/**
 * Created by kevin on 2016/8/15.
 */
public class FollowBankerResponse extends ServerResponse {
    /**
     * 必须调用此方法设置消息号
     *
     */
    public FollowBankerResponse() {
        super(1, ConnectAPI.Game_FollowBanker_Notice);
        try {
            output.writeUTF("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            output.close();
        }
    }
}
