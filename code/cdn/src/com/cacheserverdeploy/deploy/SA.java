package com.cacheserverdeploy.deploy;

import java.util.Random;

/**
 * SA 模拟退火算法
 */
public class SA {
        private int nodeNum; // 网络结点个数，编码长度
        private int N;// 每个温度迭代步长
        private int T;// 降温次数
        private float a;// 降温系数
        private float t0;// 初始温度

        private int bestT;// 最佳出现代数

        private int[] Ghh;// 初始路径编码
        private int GhhEvaluation;
        private int[] bestGh;// 最好的路径编码
        private int bestEvaluation;
        private int[] tempGhh;// 存放临时编码
        private int tempEvaluation;

        public static int minCostOfBf;

        private Random random;

        public SA() {

        }


        public SA(int cn, int t) {
            nodeNum = cn;
            if(cn < 200){
                t0 = 250.0f;
            }else if(cn >= 200 && cn < 500){
                t0 = 150.0f;
            }else if(cn >= 500){
                t0 = 250.0f;
            }
            if(cn < 200){
                N = 40;
            }else if(cn >= 200 && cn < 500) {
                N = 40;
            }else if(cn >= 500){
                N = 50;
            }

            T = t;
            if(cn < 500){
                a = 0.98f;
            }else if(cn >= 500){
                a = 0.85f;
            }
        }

        // 给编译器一条指令，告诉它对被批注的代码元素内部的某些警告保持静默
        @SuppressWarnings("resource")
        /**
         * 初始化
         */
        public void init(){
            Ghh = new int[nodeNum];
            bestGh = new int[nodeNum];
            bestEvaluation = Integer.MAX_VALUE;
            tempGhh = new int[nodeNum];
            tempEvaluation = Integer.MAX_VALUE;
            bestT = 0;
            random = new Random(System.currentTimeMillis());
        }

        // 初始化编码Ghh，以在每个与消费节点相连的网络节点都部署服务器为初始编码，初始解的生成
        //按消费节点的0.6倍部署服务器
        void initGroup(int[] connectNetwork) {
            double probability = 0.6;
            if(nodeNum > 500){  //大规模网络暂时这样
                probability = 0.75;
            }
            for (int i = 0; i < nodeNum; i++) {
                for (int j = 0; j < connectNetwork.length; j++) {
                    if(i == connectNetwork[j]){
                        if(Math.random() <= probability) {
                            Ghh[i] = 1;
                        }
                    }
                }
            }
        }

        // 复制编码体，复制编码Gha到Ghb
        public void copyGh(int[] Gha, int[] Ghb) {
            for (int i = 0; i < nodeNum; i++) {
                Ghb[i] = Gha[i];
            }
        }

        //计算部署方案最小花费
        public int evaluate(int[] chr) {
            return ZKW.calculCost(chr);
        }

        // 邻域交换，得到邻居,关于服务器部署
        // 新解产生，结合种群规模来做
        public void Linju(int[] Gh, int[] tempGh) {

            for (int i = 0; i < nodeNum; i++) {
                tempGh[i] = Gh[i];
            }

            int step = 0;   //产生新解时的变化步长,根据网络规模不同，变化步长不同

            if(nodeNum < 200){
                step = 1;
            }else if(nodeNum >= 200 && nodeNum < 500){
                step = 1;
            }else if(nodeNum >= 500){
                step = 1;
            }

            for (int j = 0; j < step; j++) {
                //寻找变异位置
                int at = random.nextInt(65535) % nodeNum; //random return [0,1)
                //变异后的值
                int bool = tempGh[at]^1;
                tempGh[at] = bool;
            }
        }

        public int[] solve(int[] connectedNetwork) {
            // 初始化编码Ghh
            initGroup(connectedNetwork);
            copyGh(Ghh, bestGh);// 复制当前编码Ghh到最好编码bestGh
            bestEvaluation = evaluate(Ghh); //计算当前编码的最小花费
            //如果初始编码没有解，则最初的最小花费设置为最大值
            if(bestEvaluation == -1){
                bestEvaluation = Integer.MAX_VALUE;
            }
            GhhEvaluation = bestEvaluation;
            int k = 0;// 降温次数
            int n = 0;// 迭代步数
            float t = t0;   //初始温度
            float r = 0.0f;
            long start_time = System.currentTimeMillis(); //循环开始时间，加入计时器
            outer: while (k < T) {
                n = 0;
                while (n < N) {//N:每个温度迭代步长
                    Linju(Ghh, tempGhh);// 得到当前编码Ghh的邻域编码tempGhh
                    tempEvaluation = evaluate(tempGhh);
                    r = random.nextFloat();
                    if(tempEvaluation != -1) {
                        if (tempEvaluation < bestEvaluation) {
                            copyGh(tempGhh, bestGh);
                            bestT = k;
                            bestEvaluation = tempEvaluation;
                        }
//                        if(count < 2000) { //大规模用例不接受较差的解
                        if (tempEvaluation < GhhEvaluation || Math.exp((GhhEvaluation - tempEvaluation) / t) > r) { //t = 250
//                            System.out.println("接受较差解");
                                copyGh(tempGhh, Ghh);
                                GhhEvaluation = tempEvaluation;
                            }
//                        }
                    }
//                    else{
//                        System.out.println("部署方案不符合。");
//                    }
                    n++;
                    if(nodeNum < 500) {
                        if (System.currentTimeMillis() - start_time > 88000) {
//                        System.out.println("模拟退火运行时间已达80s,退出。");
                            break outer;    //跳出两层循环
                        }
                    }else{
                        if (System.currentTimeMillis() - start_time > 82000) {
                            break outer;    //跳出两层循环
                        }
                    }
                }
                t = a * t;  //a:降温系数=0.98
                k++;    //降温次数加1
            }

            //获得部署方案和最小花费，进行返回
//            System.out.println("模拟退火算法结束:"+count);
//            minCostOfBf = ZKW.calculCost(bestGh);
            return bestGh;
        }
}
