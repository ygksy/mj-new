package com.gsk.mj.msghandle.pojo;


import com.gsk.mj.base.GameObj;
import com.gsk.mj.net.session.GameSession;
import com.gsk.mj.repository.AccountRepository;
import com.gsk.mj.util.AsyncTaskQueue;
import com.gsk.mj.util.GlobalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kevin on 2016/6/18.
 */
public class Avatar implements GameObj {

    @Autowired
    private AccountRepository accountRepository;

    public AvatarVO avatarVO;
    //请求吃
    public boolean chiQuest = false;
    //存储吃牌的信息
    public CardVO cardVO = new CardVO();
    //请求碰
    public boolean pengQuest = false;
    //请求杠
    public boolean gangQuest = false;
    //请求胡
    public boolean huQuest = false;
    
    //当前玩家能否吃
    public boolean canHu = true;
    
    public boolean qiangHu = true;
    /**
     * 是否是一局结算时掉线
     */
    public boolean overOff = false;
    /**
     * 存储某一句游戏断线时 结算信息
     */
    public String oneSettlementInfo;
    
    //当自己摸牌时检测自己是否有杠的牌。把杠的牌放入到整个list里面，然后在转入给前端
    public List<Integer> gangIndex = new  ArrayList<Integer>();
    /**
     * 检测到有人胡牌时存储胡牌的详细消息(划水麻将和长沙麻将用)
     *      0: 天胡
     *             1：明杠 	 1*3番
					2：暗杠	 2*3番
					3：放杠/点杠 	   1*3番（谁放牌谁出番）
					4：自摸 	6番（其他三家每家出2番）
					5：普通点炮	3番（谁放炮谁出番）
					6：七对点炮	3*3番（谁放炮谁出番）
					7：七对自摸	18番（其他三家每家出6番）
					8：杠开/杠上花	18番（其他三家每家出6番）
					9：抢杠	3*3番（谁要杠牌谁出番）
					10：一炮双响	根据胡牌者的牌型来计算放炮者出的番数（胡牌两方所胡牌型的番数相加）
					11：一炮三响	根据胡牌者的牌型来计算放炮者出的番数（同上）
					
				数组格式   牌索引:类型
				放弃的时候需要清空
		*
		*小胡点炮：3
		*大胡点炮：3*3
		*
		*小胡自摸： 2*3   （每家2分）
		*大胡自摸： 2*3*3  (每家6分)
		*
		*
		*
		* 
		* 
		* 		
     */
    public List<String> huAvatarDetailInfo = new ArrayList<String>();
    
    

	public CardVO getCardVO() {
		return cardVO;
	}

	public void setCardVO(CardVO cardVO) {
		this.cardVO = cardVO;
	}

	public RoomVO getRoomVO() {
        return roomVO;
    }

    public void setRoomVO(RoomVO roomVO) {
        this.roomVO = roomVO;
        if(avatarVO != null){
            avatarVO.setRoomId(roomVO.getRoomId());
        }
    }

    private RoomVO roomVO;
    /**
     * session
     */
    private GameSession session;
    /**
     * 牌类别 3-不含字牌，4-含字牌
     */
    private int card_row = 2;
    /**
     * 牌字
     */
    private int card_col = 27;
    /**
     * 日志记录
     */
    private static final Logger logger = LoggerFactory.getLogger(Character.class);
    /**
     * 异步操作队列
     */
    private AsyncTaskQueue asyncTaskQueue = new AsyncTaskQueue();
    /**
     * list里面字符串规则 
     * 杠：uuid(出牌家),介绍(明杠，暗杠)  （123，明杠）
     * 自己摸来杠：介绍(明杠，暗杠)
     * 
     * 点炮：uuid(出牌家),介绍(胡的类型) （123，qishouhu）
     * 自摸：介绍(胡的类型)
     * 
     * 碰：
     * 
     * eg:  碰： key:1     value: 碰的牌的下标
     * 			杠：key:2    value: 杠的牌的下标
     * 			胡：key:3    value: 胡的牌的下标
     *         吃：key:4    value:  吃的牌的下标(1:2:3)
     * 
     * key:1:碰    2:杠    3:胡   4:吃   5:抢胡
     * value:信息，分条信息之间用","隔开
     */
    private Map<Integer,String> resultRelation = new HashMap<Integer,String>(); 

    public Map<Integer,String> getResultRelation() {
		return resultRelation;
	}
    //存储杠和胡的信息
	public  void putResultRelation(Integer i , String str) {
		synchronized(resultRelation){
			if(resultRelation.get(i) == null){
				resultRelation.put(i, str);
			}
			else{
				resultRelation.put(i, resultRelation.get(i)+","+str);
			}
		}
	}
	
	public Avatar(){
    }


    public void setQuestToFalse(){
        huQuest = false;
        gangQuest = false;
        pengQuest = false;
        chiQuest = false;
    }
    /**
     * 更新用户数据表信息
     * @param value
     */
    public void updateRoomCard(int value){
        int number = avatarVO.getAccount().getRoomcard();
        number += value;
        avatarVO.getAccount().setRoomcard(number);
        accountRepository.save(avatarVO.getAccount());
    }
    /**
     * 获取玩家session
     * @return
     */
    public GameSession getSession() {
        return session;
    }
    /**
     * 获取玩家session
     * @return
     */
    public void setSession(GameSession gameSession) {
         this.session = gameSession;
    }
    /**
     * 添加异步任务，针对异步操作数据库
     * @param tasks
     */
    public void addAsyncTask(Runnable... tasks){
        asyncTaskQueue.addTask(tasks);
    }

    /**
     * 获取用户uuid
     * @return
     */
    public int getUuId(){
        return avatarVO.getAccount().getUuid();
    }


    /**
     * 设置房间属性,然后清理并重新生成新的牌数据
     *   1-转转麻将，2-划水麻将，3-长沙麻将
     */
    public void CreatePaiArray(){
        if(roomVO.getRoomType() == 1){
            card_col = 27;
            if(roomVO.getHong()){
                card_col = 34;
            }
        }else if(roomVO.getRoomType() == 2){
            card_col = 34;

        }else if(roomVO.getRoomType() == 3){
            card_col = 27;
        }
        cleanPaiData();
    }

    /**
     * 清理玩家的牌数据
     */
    public void cleanPaiData(){
        if(avatarVO.getPaiArray() == null) {
            avatarVO.setPaiArray(new int[card_row][card_col]);
        }
        for(int i=0;i<card_row;i++){
            for(int k=0;k<card_col;k++){
                avatarVO.getPaiArray()[i][k] = 0;
            }
        }
    }

    /**
     * 檢測是否可以碰 大于等于2张且对应的下标不为1都可以碰
     * @param cardIndex
     * @return
     */
    public boolean checkPeng(int cardIndex){
//    	System.out.println("杠了的牌="+cardIndex+"====="+resultRelation.get(2));
//    	System.out.println("碰了的牌="+cardIndex+"====="+resultRelation.get(1));
    	boolean flag = false;
        if(avatarVO.getPaiArray()[0][cardIndex] >= 2 ){
        	if(resultRelation.get(1) == null ){
        		flag = true;
        	}
        	else{
        		String strs [] = resultRelation.get(1).split(",");
        		for (int i = 0; i < strs.length; i++) {
        			if(strs[i].equals(cardIndex+"")){
						flag  =  false;
						 i = strs.length;
					}
					else{
						flag  =  true;
					}
				}
        	}
        }
        return flag;
    }

    /**
     * 檢測是否可以杠别人出的牌/此牌对应的下标不为1（碰过了的牌）
     * @param cardIndex
     * @return
     */
    public boolean checkGang(int cardIndex){
    	boolean flag = false;
    	gangIndex.clear();//每次出牌就先清除缓存里面的可以杠的牌下标
        if(avatarVO.getPaiArray()[0][cardIndex] == 3 && avatarVO.getPaiArray()[1][cardIndex] == 0){
        	//if(resultRelation.get(1) ==null){
        		gangIndex.add(cardIndex);
        		flag = true;
        	/*}else{
        		//
        		String strs [] = resultRelation.get(1).split(",");
        		for (int i = 0; i < strs.length; i++) {
        			if(strs[i].equals(cardIndex+"")){
						flag  =  false;
						gangIndex.clear();
						i = strs.length;
					}
					else{
						gangIndex.add(cardIndex);
						flag  =  true;
					}
				}
        	}*/
        }
        return flag;
    }
    /**
     * 检测当前自己的牌是否可杠
     * @param  
     * @return
     */
    public boolean checkSelfGang(){
     	//system.out.println("杠了的牌==杠家id"+avatarVO.getAccount().getUuid()+"===="+resultRelation.get(2));
     	//system.out.println("碰了的牌==杠家id"+avatarVO.getAccount().getUuid()+"===="+resultRelation.get(1));
    	gangIndex.clear();//先清除缓存里面的可以杠的牌下标
    	//剔除掉当前以前吃，碰，杠的牌组 再进行比较
    	boolean flag = false;
    	if(!roomVO.isAddWordCard()){
    		//划水麻将没有风牌  就27
    		for (int i= 0 ; i < 27 ; i++) {
    			if (avatarVO.getPaiArray()[0][i] == 4 && avatarVO.getPaiArray()[1][i] != 2) {
    				//先判断所有4个的牌组中是否有未杠过的
    				gangIndex.add(i);
    				flag = true;
    				break;//多个杠的情况下默认杠下标最小的个牌
    				/*if(resultRelation.get(2) == null ){
    					gangIndex.add(i);
    					flag =  true;
    					i = 100;
    				}
    				else if(resultRelation.get(2) != null ){
    					String strs [] = resultRelation.get(2).split(",");
    					for (int j = 0; j < strs.length; j++) {
    						if(strs[j].equals(i+"")){
    							flag =  false;
    							gangIndex.clear();
    							j = 100;
    							i = 100;
    						}
    						else{
    							gangIndex.add(i);
    							flag =  true;
    						}
    					}
    					for (int j = 0; j < strs.length; j++) {
    						if(!strs[j].equals(i+"")){
    							gangIndex.add(i);
    						}
    					}
    				}*/
    			}
    		}
    	}
    	else{
    		//划水麻将有风牌  就 34
    		for (int i= 0 ; i < 34 ; i++) {
    			if (avatarVO.getPaiArray()[0][i] == 4 && avatarVO.getPaiArray()[1][i] != 2) {
    				//先判断所有4个的牌组中是否有未杠过的
    				gangIndex.add(i);
    				flag = true;
    				break;//多个杠的情况下默认杠下标最小的个牌
    				/*if(resultRelation.get(2) == null ){
    					gangIndex.add(i);
    					flag =  true;
    					i = 100;//多个杠的情况下默认杠下标最小的个牌
    				}
    				else if(resultRelation.get(2) != null ){
    					String strs [] = resultRelation.get(2).split(",");
    					for (int j = 0; j < strs.length; j++) {
    						if(strs[j].equals(i+"")){
    							flag =  false;
    							gangIndex.clear();
    							j = 100;
    							i = 100;
    						}
    						else{
    							gangIndex.add(i);
    							flag =  true;
    						}
    					}
    					for (int j = 0; j < strs.length; j++) {
    						if(!strs[j].equals(i+"")){
    							gangIndex.add(i);
    						}
    					}
    				}*/
    			}
    		}
    	}
        return flag;
    }
    /**
     * 檢測是否可以吃
     * @param cardIndex
     * @return
     */
    public boolean checkChi(int cardIndex){
    	boolean flag = false;
    	//只有长沙麻将有吃的打法
    	//system.out.println("判断吃否可以吃牌-----cardIndex:"+cardIndex);
    	/**
    	 * 这里检测吃的时候需要踢出掉碰 杠了的牌****
    	 */
    	int []  cardList = avatarVO.getPaiArray()[0];
    	if(cardIndex>=0  && cardIndex <=8){
    		if(cardIndex == 0 && cardList[1] >=1 && cardList[2] >=1 ){
    			flag = true;
    		}
    		else if(cardIndex == 1 && ((cardList[0] >=1 && cardList[2] >=1) 
    				|| (cardList[3] >=1 && cardList[2] >=1))){
    			flag = true;
    		}
    		else if(cardIndex ==8 && cardList[7] >=1 && cardList[6] >=1){
    			flag = true;
    		}
    		else if(cardIndex ==7 && ((cardList[8] >=1 && cardList[6] >=1)
    				|| (cardList[5] >=1 && cardList[6] >=1))){
    			flag = true;
    		}
    		else if(cardIndex >=11 && cardIndex <= 15){
    		  if((cardList[cardIndex-1] >=1 && cardList[cardIndex+1] >=1)
    				|| (cardList[cardIndex-1] >=1 && cardList[cardIndex-2] >=1) 
    				|| (cardList[cardIndex+1] >=1 && cardList[cardIndex+2] >=1)){
    			    flag = true;
    		  }
    		}
    	}
    	else if((cardIndex>=9 && cardIndex <=17)){
    		if(cardIndex == 9 && cardList[10] >=1 && cardList[11] >=1 ){
    			flag = true;
    		}
    		else if(cardIndex == 10 && ((cardList[9] >=1 && cardList[11] >=1) 
    				|| (cardList[11] >=1 && cardList[12] >=1))){
    			flag = true;
    		}
    		else if(cardIndex ==17 && cardList[16] >=1 && cardList[15] >=1){
    			flag = true;
    		}
    		else if(cardIndex ==16 && ((cardList[15] >=1 && cardList[17] >=1)
    				|| (cardList[14] >=1 && cardList[15] >=1))){
    			flag = true;
    		}
    		else if(cardIndex >=11 && cardIndex <= 15){
    		  if((cardList[cardIndex-1] >=1 && cardList[cardIndex+1] >=1)
    				|| (cardList[cardIndex-1] >=1 && cardList[cardIndex-2] >=1) 
    				|| (cardList[cardIndex+1] >=1 && cardList[cardIndex+2] >=1)){
    			flag = true;
    		  }
    		}
    	}
    	else if(cardIndex>=18 && cardIndex <=27){
    		if(cardIndex == 18 && cardList[19] >=1 && cardList[20] >=1 ){
    			flag = true;
    		}
    		else if(cardIndex == 19 && ((cardList[18] >=1 && cardList[20] >=1) 
    				|| (cardList[20] >=1 && cardList[21] >=1))){
    			flag = true;
    		}
    		else if(cardIndex ==27 && cardList[26] >=1 && cardList[25] >=1){
    			flag = true;
    		}
    		else if(cardIndex ==26 && ((cardList[25] >=1 && cardList[27] >=1)
    				|| (cardList[24] >=1 && cardList[25] >=1))){
    			flag = true;
    		}
    		else if(cardIndex >=20 && cardIndex <= 25){
	    		 if((cardList[cardIndex-1] >=1 && cardList[cardIndex+1] >=1)
	    			|| (cardList[cardIndex-1] >=1 && cardList[cardIndex-2] >=1) 
	    			|| (cardList[cardIndex+1] >=1 && cardList[cardIndex+2] >=1)){
	    			 flag = true;
    		  } 
    		}
    	}
        return flag;
    }

    /**
     * 为自己的牌组里加入新牌
     * /碰 1  杠2  胡3  吃4
     * @param cardIndex
     */
    public boolean putCardInList(int cardIndex){
        if(avatarVO.getPaiArray()[0][cardIndex]<4) {
            avatarVO.getPaiArray()[0][cardIndex]++;
           return true;
        }else{
        	//再没检测出为什么牌组里面已经有4张牌的错误消息前暂且注释掉
            //System.out.println("Error : putCardInList --> 牌数组里已经有4张牌");
            //try {  
                //session.sendMsg(new ErrorResponse(ErrorCode.Error_000008));
           // } catch (IOException e) {
            //    e.printStackTrace();
           // }
            return true;
        }
    }

    /**
     * 设置牌的状态
     * @param cardIndex
     * @param type;//碰 1  杠2  胡3  吃4
     */
    public void setCardListStatus(int cardIndex,int type){
        avatarVO.getPaiArray()[1][cardIndex] = type;
    }

    /**
     * 把牌从自己的牌列中去除
     * @param cardIndex
     */
    public void pullCardFormList(int cardIndex){
        if(avatarVO.getPaiArray()[0][cardIndex]>0) {
            avatarVO.getPaiArray()[0][cardIndex]--;
        }else{/*
            try {
                session.sendMsg(new ErrorResponse(ErrorCode.Error_000007));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Error : pullCardFormList --> 牌数组里没有这张牌");
        */}
    }

    /**
     * 得到牌组的一维数组。用来判断是否胡牌和听牌用
     * @return
     */
    public int[][] getPaiArray(){
            return avatarVO.getPaiArray();
    }

    public int[] getSinglePaiArray() {
        int[] result = GlobalUtil.CloneIntList(avatarVO.getPaiArray()[0]);
        return result;
    }

    public String printPaiString(){
        String sb = "";
        for(int a=0;a<2;a++) {
             sb += "[";
            for (int i = 0; i < avatarVO.getPaiArray()[a].length; i++) {
                sb += avatarVO.getPaiArray()[a][i]+",";
            }
            sb += "]";
        }
        return sb;
    }
    
    public void destroyObj() {
    	//统计在线用户****
        logger.info("Avatar用户{}断开服务器链接",avatarVO.getAccount().getNickname());
        if(session != null){
        	//session.clearAllInfo();
        }
    }
    /**
     * kevinTest
     * 测试胡牌用的数据
     * @return
     */
    public int[] getTestPaiArray(){
        return new int[]{0, 2, 2, 2, 4, 1, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }

}
