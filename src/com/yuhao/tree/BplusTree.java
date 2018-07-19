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


    protected String ROOT_NAME="root";

    protected String HASH_NAME="hash";

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

    protected String path;


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

    public BplusTree(String path,int order){

        this.order=order;
        this.path=path;

        tmp=new HashMap<>();
        File hashFile=new File(this.path+this.HASH_NAME);
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

        if (hashMap.get(this.ROOT_NAME)==null){
            root = new BplusNode<K, V>(true, true,this.ROOT_NAME,this.path+this.ROOT_NAME);
            head = root;
        }else {

            File rootFile=new File(hashMap.get(this.ROOT_NAME));
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
                new BplusNode<K, V>(true, true,this.ROOT_NAME,this.path+this.ROOT_NAME);
            }
        }
        tmp.put(this.ROOT_NAME,root);

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

    // 测试
    public static void main(String[] args) throws IOException, ClassNotFoundException {



//      testRandomInsert(10000);




		 testRandomSearch("280");

//	 testRandomRemove("530");

    }


    private static void testRandomRemove(String key) throws IOException, ClassNotFoundException {
        BplusTree<String, String> tree = new BplusTree<String, String>("F:/b/",100);

        System.out.println("Begin random remove...");
        long current = System.currentTimeMillis();

        System.out.println(tree.remove(key));


        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);

        System.out.println(tree.i);

        tree.finish();


    }

    private static void testRandomSearch(String key) throws IOException, ClassNotFoundException {
        BplusTree<String, String> tree = new BplusTree<String, String>("F:/b/",100);

        System.out.println("Begin random search...");
        long current = System.currentTimeMillis();

        System.out.println(tree.get("280"));

        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);

        System.out.println(tree.i);

    }

    private static void testRandomInsert(int size) throws IOException, ClassNotFoundException {
        BplusTree<String, String> tree = new BplusTree<String, String>("F:/b/",100);
        Random random = new Random();

        long current = System.currentTimeMillis();

        for (int i = 0; i <size ; i++) {
            tree.insertOrUpdate(random.nextInt(size)+"",UUID.randomUUID().toString());
        }


        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);

        System.out.println(tree.getHeight());

        System.out.println(tree.i);

        tree.finish();

    }

    private void finish() throws IOException {
        File file=new File(this.path+this.HASH_NAME);
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(hashMap);
        oos.close();
    }

}
