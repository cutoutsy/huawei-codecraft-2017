package com.cacheserverdeploy.deploy;

import java.util.*;

/**
 * ZKW 算法计算最小费用最大流
 */
public class ZKW {

    static class EdgeZ
    {
        int cost, cap, v;
        int next, re;
        int cost_temp;
        int cap_temp;
    }
    public static EdgeZ[] edge_zkw = new EdgeZ[88000];


    static int ans, cost_zkw, src, des, n;
    static int maxFlow;
    static int cnt_zkw;
    static int[] head_zkw = new int[1002];
    static int[] dist = new int[1002];
    static boolean[] vis = new boolean[1002];

    public static int serverCost;
    public static int consumeTotal;

    public ZKW(int serverCost){
        this.serverCost = serverCost;
        this.consumeTotal = consumeTotal;
    }

    void init_zkw()
    {
        Arrays.fill(head_zkw, -1);
        cnt_zkw = 0;
        ans = cost_zkw = 0;
        for (int i = 0; i < edge_zkw.length; i++) {
            edge_zkw[i] = new EdgeZ();
        }
    }

    void setConsume(int[] consume){
        int sum = 0;
        for (int i = 0; i < consume.length; i++) {
            sum += consume[i];
        }
        this.consumeTotal = sum;
    }

    static void add(int u, int v, int cap, int cost)
    {
        edge_zkw[cnt_zkw].v = v;
        edge_zkw[cnt_zkw].cap = cap;
        edge_zkw[cnt_zkw].cost = cost;
        edge_zkw[cnt_zkw].re = cnt_zkw + 1;
        edge_zkw[cnt_zkw].next = head_zkw[u];
        head_zkw[u] = cnt_zkw++;

        edge_zkw[cnt_zkw].v = u;
        edge_zkw[cnt_zkw].cap = 0;
        edge_zkw[cnt_zkw].cost = -cost;
        edge_zkw[cnt_zkw].re = cnt_zkw - 1;
        edge_zkw[cnt_zkw].next = head_zkw[v];
        head_zkw[v] = cnt_zkw++;

        edge_zkw[cnt_zkw-2].cap_temp=cap;
        edge_zkw[cnt_zkw-2].cost_temp=cost;
        edge_zkw[cnt_zkw-1].cap_temp=0;
        edge_zkw[cnt_zkw-1].cost_temp=-cost;
    }

    static int aug(int u, int f)
    {
        if(u == des)
        {
            ans += cost_zkw * f;
            maxFlow+=f;
            return f;
        }
        vis[u] = true;
        int tmp = f;
        for(int i = head_zkw[u]; i != -1; i = edge_zkw[i].next)
            if(edge_zkw[i].cap>0 && (edge_zkw[i].cost==0) && !vis[edge_zkw[i].v])
            {
                int delta = aug(edge_zkw[i].v, tmp < edge_zkw[i].cap ? tmp : edge_zkw[i].cap);
                edge_zkw[i].cap -= delta;
                edge_zkw[edge_zkw[i].re].cap += delta;
                tmp -= delta;
                if(tmp==0) return f;
            }
        return f - tmp;
    }
    static boolean modlabel()
    {
        for(int i = 0; i < n; i++) {
            dist[i] = Integer.MAX_VALUE;
        }
        dist[des] = 0;
        Deque<Integer> Q = new ArrayDeque<>();
        Q.addLast(des);
        while(!Q.isEmpty())
        {
            int u = Q.getFirst();
            Q.pollFirst();
            int tmp;
            for(int i = head_zkw[u]; i != -1; i = edge_zkw[i].next) {
                if (edge_zkw[edge_zkw[i].re].cap > 0 && (tmp = dist[u] - edge_zkw[i].cost) < dist[edge_zkw[i].v]) {
//                    (dist[edge_zkw[i].v] = tmp) <= dist[Q.isEmpty() ? src : Q.getFirst()] ? Q.addFirst(edge_zkw[i].v) : Q.addLast(edge_zkw[i].v);
                    if ((dist[edge_zkw[i].v] = tmp) <= dist[Q.isEmpty() ? src : Q.getFirst()]){
                        Q.addFirst(edge_zkw[i].v);
                    }else{
                        Q.addLast(edge_zkw[i].v);
                    }
                }
            }

        }
        for(int u = 0; u < n; u++)
            for(int i = head_zkw[u]; i != -1; i = edge_zkw[i].next)
                edge_zkw[i].cost += dist[edge_zkw[i].v] - dist[u];
        cost_zkw += dist[src];
        return dist[src] < Integer.MAX_VALUE;
    }
    static void costflow()
    {
        while(modlabel())
        {
            do
            {
                Arrays.fill(vis, false);
            }while( aug(src, Integer.MAX_VALUE) > 0);
        }
    }

    public static int calculCost(int[] input){
        int[] a = new int[input.length];
        int sum = 0;
        for(int i=0;i < input.length;i++)
        {
            if(input[i] == 1) {
                a[sum++] = i;
            }
        }
        int[] head_temp = new int[sum];
        for (int i = 0; i < sum; i++) {
            head_temp[i] = head_zkw[a[i]];
            add(input.length, a[i], Integer.MAX_VALUE, 0);
        }
        ans = cost_zkw = 0;
        maxFlow=0;
        src=input.length;	//超级源点
        des=input.length+1;	//超级汇点
        n=input.length+2;
        costflow();

        //恢复原图
        head_zkw[input.length] = -1;
        for (int i = 0; i < sum; i++) {
            head_zkw[a[i]]=head_temp[i];
            cnt_zkw--;
            edge_zkw[cnt_zkw].v=0;
            edge_zkw[cnt_zkw].cap=0;
            edge_zkw[cnt_zkw].cost=0;
            edge_zkw[cnt_zkw].re=0;
            edge_zkw[cnt_zkw].next=0;
            cnt_zkw--;
            edge_zkw[cnt_zkw].v=0;
            edge_zkw[cnt_zkw].cap=0;
            edge_zkw[cnt_zkw].cost=0;
            edge_zkw[cnt_zkw].re=0;
            edge_zkw[cnt_zkw].next=0;
        }
        for(int i=0;i<cnt_zkw;i++)
        {
            edge_zkw[i].cap=edge_zkw[i].cap_temp;
            edge_zkw[i].cost=edge_zkw[i].cost_temp;
        }
        int mincost = -1;
        if(maxFlow == consumeTotal) {
            mincost = ans + (sum * serverCost);
        }
        return mincost;
    }
}
