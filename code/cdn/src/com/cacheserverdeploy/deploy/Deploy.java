package com.cacheserverdeploy.deploy;



import java.util.*;

public class Deploy
{
    /**
     * 你需要完成的入口
     * <功能详细描述>
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */
    public static String[] deployServer(String[] graphContent)
    {
        int networkNums = Integer.valueOf(graphContent[0].trim().split("\\s")[0]);  //网络节点数
        int linkNums = Integer.valueOf(graphContent[0].trim().split("\\s")[1]); //链路数
        int costNums = Integer.valueOf(graphContent[0].trim().split("\\s")[2]); //消费节点数

        int serverCost = Integer.valueOf(graphContent[2].trim());   //服务器部署成本

        HashMap<String, Integer> totalBandwidth = new HashMap<>();  //网络各链路总带宽表示
        HashMap<String, Integer> eachBandwidthCost = new HashMap<>();   //各链路单位带宽租用费

        SPFA spfa = new SPFA();

        ZKW zkw = new ZKW(serverCost);
        zkw.init_zkw();

        for(int i = 4; i < linkNums+4; i++){
            String oneline = graphContent[i].trim();

            String start = oneline.split("\\s")[0];
            String end = oneline.split("\\s")[1];
            int cap = Integer.valueOf(oneline.split("\\s")[2]);
            int cost = Integer.valueOf(oneline.split("\\s")[3]);

            spfa.addedge(Integer.valueOf(start), Integer.valueOf(end), cap, cost);

            zkw.add(Integer.valueOf(start),Integer.valueOf(end),cap,cost);


            zkw.add(Integer.valueOf(end), Integer.valueOf(start),cap,cost);

            totalBandwidth.put(start+":"+end, cap);  //上行带宽
            totalBandwidth.put(end+":"+start, cap);  //下行带宽
            eachBandwidthCost.put(start+":"+end, cost);
            eachBandwidthCost.put(end+":"+start, cost);
        }

        int[] connectNetwork = new int[costNums];   //与消费节点相连的网络节点
        int[] costTotal = new int[costNums];    //消费节点的带宽消费需求

        int costIndex = 0;
        for (int i = linkNums+5; i < graphContent.length; i++) {
            String oneline = graphContent[i].trim();
            int adjacent = Integer.valueOf(oneline.split("\\s")[1]);
            int consume = Integer.valueOf(oneline.split("\\s")[2]);

            spfa.addoneedge(adjacent, networkNums+1, consume, 0);
            zkw.add(adjacent, networkNums+1, consume, 0);

            connectNetwork[costIndex] = adjacent;
            costTotal[costIndex] = consume;
            costIndex += 1;
        }

        zkw.setConsume(costTotal);

        SA sa = new SA(networkNums, 3000);
        sa.init();

        int[] bestSolution = sa.solve(connectNetwork);

        Map<LinkedList<Integer>, Integer> reSolution = spfa.canculate_path(bestSolution);
        Map<LinkedList<Integer>, Integer> endSolution = new HashMap<LinkedList<Integer>, Integer>();

        for (Map.Entry<LinkedList<Integer>, Integer> entry : reSolution.entrySet()){
            LinkedList<Integer> tempPath = entry.getKey();
            LinkedList<Integer> newTempPath = new LinkedList<>();
            for (int i = tempPath.size()-1; i >= 0; i--) {
                if(tempPath.get(i) != (networkNums) ){
                    newTempPath.add(tempPath.get(i));
                }
            }
            endSolution.put(newTempPath, entry.getValue());
        }

        String[] re = generateResultStringArray(endSolution, connectNetwork);
        return re;
    }

    //解决方案验证,同时验证带宽和花费是否正确
    public static boolean solutionIsValid(Map<LinkedList<Integer>, Integer> solution, int mincost, int serverCost, HashMap<String, Integer> totalBandwidth, HashMap<String, Integer> eachBandwidthCost, int[] connectNetwork, int[] costTotal){
//        验证所有消费节点的带宽是否全部满足
        if(!isSatisfiedAllCost(solution, connectNetwork, costTotal)){
            System.out.println("消费节点带宽没有全部满足");
            return false;
        }
//        验证路径带宽是否超过链路最大带宽
        if(!isBandwidthSatisfied(solution, totalBandwidth)){
            System.out.println("路径带宽是否超过链路最大带宽");
            return false;
        }
//        验证解决方案的最小花费是否正确
//        if(!isMincostSatisfied(solution, mincost, serverCost, eachBandwidthCost)){
//            System.out.println("最小花费与路径计算的不符合");
//            return false;
//        }
        return true;
    }

    //解决方案带宽是否满足所有的消费节点
    public static boolean isSatisfiedAllCost(Map<LinkedList<Integer>, Integer> solution, int[] connectNetwork, int[] costTotal){
        boolean re = true;
        int[] temp = new int[costTotal.length];
        for (int i = 0; i < costTotal.length; i++) {
            temp[i] = costTotal[i];
        }
        for (Map.Entry<LinkedList<Integer>, Integer> entry : solution.entrySet()){
            System.out.println(entry.getKey()+":"+entry.getValue());
            LinkedList<Integer> path = entry.getKey();
            int usedBandwidth = entry.getValue();
            int lastNetwork = path.getLast();
            for (int i = 0; i < connectNetwork.length; i++) {
                if (connectNetwork[i] == lastNetwork){
                    temp[i] = temp[i] - usedBandwidth;
                }
            }
        }

        for (int i = 0; i < temp.length; i++) {
            if(temp[i] != 0){
                System.out.println("满足节点消费带宽有问题");
                re = false;
            }
        }
        return re;
    }

    //验证路径带宽是否超过链路最大带宽
    public static boolean isBandwidthSatisfied(Map<LinkedList<Integer>, Integer> solution, HashMap<String, Integer> totalBandwidth){
        HashMap<String, Integer> tempTotalBandwidth = new HashMap<>();
        for (Map.Entry<String, Integer> entry : totalBandwidth.entrySet()){
            tempTotalBandwidth.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<LinkedList<Integer>, Integer> entry : solution.entrySet()) {
            LinkedList<Integer> path = entry.getKey();
            int usedBandwidth = entry.getValue();
            if(path.size() > 0){
                for (int i = 0; i < path.size()-1; i++) {
                    String key = path.get(i)+":"+path.get(i+1);
                    tempTotalBandwidth.put(key, tempTotalBandwidth.get(key)-usedBandwidth);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : tempTotalBandwidth.entrySet()){
//            tempTotalBandwidth.put(entry.getKey(), entry.getValue());
            if(entry.getValue() < 0){
                System.out.println("edge:"+entry.getKey()+"超出流量:"+entry.getValue());
                return true;
            }
        }
        return true;
    }

    //验证解决方案的最小花费是否正确
    public static boolean isMincostSatisfied(Map<LinkedList<Integer>, Integer> solution, int mincost, int serverCost, HashMap<String, Integer> eachBandwidthCost){
        int cost = 0;
        for (Map.Entry<LinkedList<Integer>, Integer> entry : solution.entrySet()) {
            LinkedList<Integer> path = entry.getKey();
            int usedBandwidth = entry.getValue();
            if(path.size() > 1){
                for (int i = 0; i < path.size()-1; i++) {
                    String key = path.get(i)+":"+path.get(i+1);
                    cost += eachBandwidthCost.get(key) * usedBandwidth;
                }
            }
        }
        Set<Integer> server = new HashSet<>();
        for (Map.Entry<LinkedList<Integer>, Integer> entry : solution.entrySet()) {
            LinkedList<Integer> path = entry.getKey();
            int lastNetwork = Integer.valueOf(path.getFirst());
            server.add(lastNetwork);
        }
        cost += serverCost*server.size();
        if(cost == mincost){
            return true;
        }else{
            System.out.println("根据路径计算出来的花费:"+cost);
            return false;
        }
    }

    //生成最后返回的结果数组
    public static String[] generateResultStringArray(Map<LinkedList<Integer>, Integer> solution, int[] connectNetwork){
        String[] re = new String[solution.size()+2];
        if(solution.size() > 0) {
            re[0] = String.valueOf(solution.size());
            re[1] = "";
            int index = 2;
            for (Map.Entry<LinkedList<Integer>, Integer> entry : solution.entrySet()){
                LinkedList<Integer> path = entry.getKey();
                int pathCost = entry.getValue();
                StringBuffer eachOne = new StringBuffer();
                for (int i = 0; i < path.size(); i++) {
                    eachOne.append(path.get(i)+" ");
                }
                int networkConnectedCost = Integer.valueOf(path.get(path.size() - 1));
                int costIndex = 0;  //消费节点，这块可以优化，构造一个键值对，避免循环
                for (int i = 0; i < connectNetwork.length; i++) {
                    if (connectNetwork[i] == networkConnectedCost) {
                        costIndex = i;
                        break;  //找到就跳出循环
                    }
                }
                eachOne.append(costIndex+" ");
                eachOne.append(pathCost);
                re[index++] = eachOne.toString();
            }
        }
        return re;
    }
}
