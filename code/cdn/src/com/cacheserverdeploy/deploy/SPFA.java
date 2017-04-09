package com.cacheserverdeploy.deploy;

import java.util.*;

/**
 * SPFA算法输出正确路径
 */
public class SPFA {

    static class EdgeS{
        int u, v, cap, flow, cost, next;
    }

    public static EdgeS[] edge = new EdgeS[22000];

    int cnt;
    static int[] head = new int[1002];
    int[] path = new int[1002];
    int[] dist = new int[1002];
    boolean[] vis = new boolean[1002];

    public SPFA(){
        this.cnt = 0;
        for (int i = 0; i < edge.length; i++) {
            edge[i] = new EdgeS();
        }
        Arrays.fill(head, -1);
    }

    public void addedge(int u,int v,int cap,int cost)
    {
        edge[cnt].u=u;
        edge[cnt].v=v;
        edge[cnt].cap=cap;
        edge[cnt].cost=cost;
        edge[cnt].next=head[u];
        head[u]=cnt++;

        edge[cnt].u=v;
        edge[cnt].v=u;
        edge[cnt].cap=cap;
        edge[cnt].cost=cost;
        edge[cnt].next=head[v];
        head[v]=cnt++;
    }

    public void addoneedge(int u,int v,int cap,int cost)
    {
        edge[cnt].u=u;
        edge[cnt].v=v;
        edge[cnt].cap=cap;
        edge[cnt].cost=cost;
        edge[cnt].next=head[u];
        head[u]=cnt++;
    }

    public Map<LinkedList<Integer>, Integer> canculate_path(int[] serverPosition)
    {
        int nodesNum = serverPosition.length;
        Vector<Integer> a = new Vector<>();//存选中的服务器
        Map<LinkedList<Integer>, Integer> solution = new HashMap<LinkedList<Integer>, Integer>();
        for(int i=0;i<serverPosition.length;i++)//存选中的服务器
        {
            if(serverPosition[i]==1)	//最好基因
            {
                a.add(i);
            }
        }

        //添加超级源点
        for(int i=0;i<a.size();i++)
        {
            addoneedge(nodesNum,a.get(i),Integer.MAX_VALUE,0);
        }

        //SPFA找路径
        int i = 0;
        int minflow = 0;
        int mincost=0,maxflow=0;//最小流最大流=0

        while(SPFA(nodesNum,nodesNum+1,nodesNum+2))//nodesNum+2表示增加超级源点和超级汇点后的节点数
        {
            minflow=Integer.MAX_VALUE;	//minflow设置为极大值
            for(i=path[nodesNum+1];i!=-1;i=path[edge[i].u])
                if(edge[i].cap-edge[i].flow < minflow)
                    minflow=edge[i].cap-edge[i].flow;
            LinkedList<Integer> tempPath = new LinkedList<>();
            for(i=path[nodesNum+1];i!=-1;i=path[edge[i].u])
            {
                edge[i].flow+=minflow;
                tempPath.add(edge[i].u);
            }
            solution.put(tempPath, minflow);
        }

        return solution;
    }

    boolean SPFA(int s,int t,int n)
    {
        int i,u,v;
        Stack<Integer> qu = new Stack<>();
        Arrays.fill(vis, false);
        Arrays.fill(path, -1);
        for(i=0;i<=n;i++)
            dist[i]=Integer.MAX_VALUE;
        vis[s]=true;
        dist[s]=0;
        qu.push(s);
        while(!qu.empty())
        {
            u=qu.pop();
            vis[u]=false;
            for(i=head[u];i!=-1;i=edge[i].next)
            {
                v=edge[i].v;
                if(edge[i].cap>edge[i].flow &&dist[v]>dist[u]+edge[i].cost)
                {
                    dist[v]=dist[u]+edge[i].cost;
                    path[v]=i;//这里存的边的序号
                    if(!vis[v])
                    {
                        qu.push(v);
                        vis[v]=true;
                    }
                }
            }
        }
        if(dist[t]==Integer.MAX_VALUE)	//t不可达，即没有增广路径了，返回false
        {
            return false;
        }
        return true;
    }
}
