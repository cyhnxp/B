package com.yuhao.tree;
/**
 * B+树的定义：
 *
 * 1.任意非叶子结点最多有M个子节点；且M>2；M为B+树的阶数
 * 2.除根结点以外的非叶子结点至少有 (M+1)/2个子节点；
 * 3.根结点至少有2个子节点；
 * 4.除根节点外每个结点存放至少（M-1）/2和至多M-1个关键字；（至少1个关键字）
 * 5.非叶子结点的子树指针比关键字多1个；
 * 6.非叶子节点的所有key按升序存放，假设节点的关键字分别为K[0], K[1] … K[M-2],
 *  指向子女的指针分别为P[0], P[1]…P[M-1]。则有：
 *  P[0] < K[0] <= P[1] < K[1] …..< K[M-2] <= P[M-1]
 * 7.所有叶子结点位于同一层；
 * 8.为所有叶子结点增加一个链指针；
 * 9.所有关键字都在叶子结点出现
 */


import java.io.*;
import java.util.*;

public class BplusTree <K extends Comparable<K>, V>{

    /** 根节点 */
    protected BplusNode<K, V> root;

    /** 阶数，M值 */
    protected int order=100;

    /** 叶子节点的链表头 */
    protected BplusNode<K, V> head;

    /** 树高*/
    protected int height = 0;

    protected HashMap<String,String> hashMap;

    protected HashMap<String,BplusNode> tmp;

    public int i=0;

    public BplusNode<K, V> getHead() {
        return head;
    }

    public void setHead(BplusNode<K, V> head) {
        this.head = head;
    }

    public BplusNode<K, V> getRoot() {
        return root;
    }

    public void setRoot(BplusNode<K, V> root) {
        this.root = root;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public V get(K key) throws IOException, ClassNotFoundException {
        return root.get(key,this);
    }

    public V remove(K key) throws IOException, ClassNotFoundException {
        return root.remove(key, this);
    }

    public void insertOrUpdate(K key, V value) throws IOException, ClassNotFoundException {
        root.insertOrUpdate(key, value, this);

    }

    public BplusTree(){

        tmp=new HashMap<>();

        File hashFile=new File("hash");
        if (!hashFile.exists()){
            hashFile.exists();
        }
        try {
            ObjectInputStream hsah = new ObjectInputStream(new FileInputStream(hashFile));
            hashMap= (HashMap<String, String>) hsah.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (hashMap==null){
            hashMap=new HashMap<>();
        }

        if (hashMap.get("root")==null){
            root = new BplusNode<K, V>(true, true,"root","root");
            head = root;
        }else {

            File rootFile=new File(hashMap.get("root"));
            ObjectInputStream hsah=null;
            try {
                 hsah = new ObjectInputStream(new FileInputStream(rootFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                BplusNode root= (BplusNode) hsah.readObject();
                this.i++;
                this.root=root;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (root==null){
                new BplusNode<K, V>(true, true,"root","root");
            }

        }

        tmp.put("root",root);

    }


    public BplusNode getNode(String name) throws IOException, ClassNotFoundException {

        BplusNode node=tmp.get(name);
        if (node!=null)
        {
            return node;
        }

        File file=new File(hashMap.get(name));
        ObjectInputStream hsah = new ObjectInputStream(new FileInputStream(file));

        BplusNode newNode=(BplusNode) hsah.readObject();

        tmp.put(newNode.name,newNode);

        this.i++;

        return newNode;

    }

    public BplusTree(int order) {
        if (order < 3) {
            System.out.print("order must be greater than 2");
            System.exit(0);
        }
        this.order = order;
        root = new BplusNode<K, V>(true, true,"root","root");
        head = root;

    }

    // 测试
    public static void main(String[] args) throws IOException, ClassNotFoundException {



//       testRandomInsert();




		 testRandomSearch("280");

//	 testRandomRemove("530");

    }


    private static void testRandomRemove(String key) throws IOException, ClassNotFoundException {
        BplusTree<String, String> tree = new BplusTree<String, String>();



        System.out.println("Begin random remove...");
        long current = System.currentTimeMillis();

        System.out.println(tree.remove(key));


        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);

        System.out.println(tree.i);

        tree.finish();


    }

    private static void testRandomSearch(String key) throws IOException, ClassNotFoundException {
        BplusTree<String, String> tree = new BplusTree<String, String>();

        System.out.println("Begin random search...");
        long current = System.currentTimeMillis();

        System.out.println(tree.get("280"));

        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);

        System.out.println(tree.i);

    }

    private static void testRandomInsert() throws IOException, ClassNotFoundException {
        BplusTree<String, String> tree = new BplusTree<String, String>();
        Random random = new Random();

        long current = System.currentTimeMillis();

        tree.insertOrUpdate("280","9999999999999999");

        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);

        System.out.println(tree.getHeight());

        System.out.println(tree.i);

        tree.finish();

    }

    private void finish() throws IOException {
        File file=new File("hash");
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(hashMap);
        oos.close();
    }

}
