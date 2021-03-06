package com.gsk.mj.msghandle.impl;

import com.gsk.mj.constant.CommonConstant;
import com.gsk.mj.context.GameServerContext;
import com.gsk.mj.domain.Account;
import com.gsk.mj.domain.NoticeTable;
import com.gsk.mj.logic.RoomLogic;
import com.gsk.mj.msghandle.MsgProcessor;
import com.gsk.mj.msghandle.manage.GameSessionManager;
import com.gsk.mj.msghandle.manage.RoomManager;
import com.gsk.mj.msghandle.pojo.Avatar;
import com.gsk.mj.msghandle.pojo.AvatarVO;
import com.gsk.mj.msghandle.pojo.LoginVO;
import com.gsk.mj.msghandle.response.HostNoitceResponse;
import com.gsk.mj.msghandle.response.HuPaiResponse;
import com.gsk.mj.msghandle.response.LoginResponse;
import com.gsk.mj.net.model.ClientRequest;
import com.gsk.mj.net.session.GameSession;
import com.gsk.mj.repository.AccountRepository;
import com.gsk.mj.repository.NoticeTableRepository;
import com.gsk.mj.util.JsonUtilTool;
import com.gsk.mj.util.TimeUitl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;


public class LoginMsgProcessor extends MsgProcessor {
	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private NoticeTableRepository noticeTableRepository;

	@Override
	public  void process(GameSession gameSession, ClientRequest request)
			throws Exception {
		String message = request.getString();
		//读取登陆信息
		LoginVO loginVO = JsonUtilTool.fromJson(message,LoginVO.class);
		Account account = accountRepository.findByOpenid(loginVO.getOpenId());
		if(account==null) {
			//创建新用户并登录
			account = new Account();
			account.setOpenid(loginVO.getOpenId());
//			account.setUuid(AccountService.getInstance().selectMaxId() + 100000);
			account.setRoomcard(CommonConstant.initialRoomCard);
			account.setHeadicon(loginVO.getHeadIcon());
			account.setNickname(loginVO.getNickName());
			account.setCity(loginVO.getCity());
			account.setProvince(loginVO.getProvince());
			account.setSex(loginVO.getSex());
			account.setUnionid(loginVO.getUnionid());
			account.setPrizecount(CommonConstant.initialPrizeCount);
			account.setCreatetime(new Date());
			account.setActualcard(CommonConstant.initialRoomCard);
			account.setTotalcard(CommonConstant.initialRoomCard);
			account.setStatus("0");
			account.setIsGame("0");
			Account account_new = accountRepository.save(account);
			Avatar tempAva = new Avatar();
			AvatarVO tempAvaVo = new AvatarVO();
			tempAvaVo.setAccount(account);
			tempAvaVo.setIP(loginVO.getIP());
			tempAva.avatarVO = tempAvaVo;
			loginAction(gameSession,tempAva);
			//把session放入到GameSessionManager
			GameSessionManager.getInstance().putGameSessionInHashMap(gameSession,tempAva.getUuId());
			//公告发送给玩家
			Thread.sleep(3000);
			NoticeTable notice = null;
			try {
				notice = noticeTableRepository.findLast();
			} catch (Exception e) {
				e.printStackTrace();
			}
			String content = notice.getContent();
			gameSession.sendMsg(new HostNoitceResponse(1, content));
		}
		else{
			//如果玩家是掉线的，则直接从缓存(GameServerContext)中取掉线玩家的信息
			//判断用户是否已经进行断线处理(如果前端断线时间过短，后台则可能还未来得及把用户信息放入到离线map里面，就已经登录了，所以取出来就会是空)
			Thread.sleep(1000);
			Avatar avatar = GameServerContext.getAvatarFromOn(account.getUuid());
			if(avatar == null){
				avatar =  GameServerContext.getAvatarFromOff(account.getUuid());
			}
			if(avatar == null){
				GameSession gamesession = GameSessionManager.getInstance().getAvatarByUuid("uuid_"+account.getUuid());
				if(gamesession != null){
					avatar =  gamesession.getRole(Avatar.class);
				}
			}
			if(avatar == null) {
				//判断微信昵称是否修改过，若修改过昵称，则更新数据库信息
				if(!loginVO.getNickName().equals(account.getNickname())){
					account.setNickname(loginVO.getNickName());
					accountRepository.save(account);
				}
				//断线超过时间后，自动退出
				avatar = new Avatar();
				AvatarVO avatarVO = new AvatarVO();
				avatarVO.setAccount(account);
				avatarVO.setIP(loginVO.getIP());
				avatar.avatarVO = avatarVO;
				//把session放入到GameSessionManager
				loginAction(gameSession,avatar);
				GameSessionManager.getInstance().putGameSessionInHashMap(gameSession,avatar.getUuId());
				Thread.sleep(3000);
				//公告发送给玩家
				NoticeTable notice = null;
				try {
					notice = noticeTableRepository.findLast();
				} catch (Exception e) {
					e.printStackTrace();
				}
				String content = notice.getContent();
				gameSession.sendMsg(new HostNoitceResponse(1, content));
			}else{
				//断线重连
				GameServerContext.add_onLine_Character(avatar);
				GameServerContext.remove_offLine_Character(avatar);
				avatar.avatarVO.setIsOnLine(true);
				avatar.avatarVO.setAccount(account);
				avatar.avatarVO.setIP(loginVO.getIP());
				TimeUitl.stopAndDestroyTimer(avatar);
				avatar.setSession(gameSession);
				//system.out.println("用户回来了，断线重连，中止计时器");
				//返回用户断线前的房间信息******
				gameSession.setLogin(true);
				gameSession.setRole(avatar);
				returnBackAction(gameSession ,avatar);
				//把session放入到GameSessionManager,并且移除以前的session
				GameSessionManager.getInstance().putGameSessionInHashMap(gameSession,avatar.getUuId());
				//公告发送给玩家
				Thread.sleep(3000);
				NoticeTable notice = null;
				try {
					notice = noticeTableRepository.findLast();
				} catch (Exception e) {
					e.printStackTrace();
				}
				String content = notice.getContent();
				gameSession.sendMsg(new HostNoitceResponse(1, content));

			}
		}


	}


//
//			if(AccountService.getInstance().createAccount(account) == 0){
//				gameSession.sendMsg(new LoginResponse(0,null));
//				TimeUitl.delayDestroy(gameSession,1000);
//			}else{

//

//			}
//		}

//		System.out.println(account.getUuid()+"  :登录游戏");
//	}
//
	/**
	 * 登录操作
	 * @param gameSession
	 * @param avatar
     */
	public void loginAction(GameSession gameSession,Avatar avatar){
		gameSession.setRole(avatar);
		gameSession.setLogin(true);
		avatar.setSession(gameSession);
		avatar.avatarVO.setIsOnLine(true);
		GameServerContext.add_onLine_Character(avatar);
		gameSession.sendMsg(new LoginResponse(1,avatar.avatarVO));
	}
//
	/**
	 *玩家断线重连操作
	 * @param
	 * @param avatar
     */
	public void returnBackAction(GameSession gameSession ,Avatar avatar){


		if(avatar.avatarVO.getRoomId() != 0){
			RoomLogic roomLogic = RoomManager.getInstance().getRoom(avatar.avatarVO.getRoomId());
			if(roomLogic !=null){
				//如果用户是在玩游戏/在房间的时候断线，且返回时房间还未被解散，则需要返回游戏房间其他用户信息，牌组信息
				roomLogic.returnBackAction(avatar);
				try {
					Thread.sleep(1000);
					if(avatar.overOff){
						//在某一句结算时断线，重连时返回结算信息
						avatar.getSession().sendMsg(new HuPaiResponse(1,avatar.oneSettlementInfo));
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else{
				//如果是在游戏时断线,但是返回的时候，游戏房间已经被解散，则移除该用户的房间信息
//				AvatarVO avatarVO = new AvatarVO();
//				avatarVO.setAccount(avatar.avatarVO.getAccount());
//				avatarVO.setIP(avatar.avatarVO.getIP());;
				GameSession gamesession = avatar.getSession();
//				avatar = new Avatar();
//				avatar.avatarVO = avatarVO;
//				avatar.setSession(gamesession);
//				avatar.avatarVO.setIsOnLine(true);
//				gamesession.setRole(avatar);
//				gamesession.setLogin(true);
				GameServerContext.add_onLine_Character(avatar);
				gamesession.sendMsg(new LoginResponse(1, avatar.avatarVO));
			}
		}
		else{
			//如果不是在游戏时断线，则直接返回个人用户信息avatar
			avatar.getSession().sendMsg(new LoginResponse(1, avatar.avatarVO));
		}

	}

}
