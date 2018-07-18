package com.yuhao.tree;

import java.io.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;


public class BplusNode <K extends Comparable<K>, V> implements Serializable {


    private static final long serialVersionUID = 9158433948289218012L;

    protected String name;

    /** 是否为叶子节点 */
    protected boolean isLeaf;

    /** 是否为根节点*/
    protected boolean isRoot;

    /** 父节点 */
    protected  String parentName;

    /** 叶节点的前节点*/
    protected  String previousName;

    /** 叶节点的后节点*/
    protected String  nextName;

    /** 节点的关键字 */
    protected List<Entry<K, V>> entries;

    /** 子节点 */
    protected  List<String> childrenName;

    /** 本地路径**/
    protected String path;


    public BplusNode(boolean isLeaf,String name,String path) {
        this.name=name;
        this.isLeaf = isLeaf;
        this.path=path;
        entries = new ArrayList<Entry<K, V>>();

        if (!isLeaf) {
            childrenName = new ArrayList<>();
        }
    }

    public BplusNode(boolean isLeaf, boolean isRoot,String name,String path) {
        this(isLeaf,name,path);
        this.isRoot = isRoot;

    }

    public V get(K key,BplusTree tree) throws IOException, ClassNotFoundException {

        //如果是叶子节点
        if (isLeaf) {
            int low = 0, high = entries.size() - 1, mid;
            int comp ;
            while (low <= high) {
                mid = (low + high) / 2;
                comp = entries.get(mid).getKey().compareTo(key);
                if (comp == 0) {
                    return entries.get(mid).getValue();
                } else if (comp < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            //未找到所要查询的对象
            return null;
        }
        //如果不是叶子节点
        //如果key小于节点最左边的key，沿第一个子节点继续搜索
        if (key.compareTo(entries.get(0).getKey()) < 0) {
            BplusNode children=tree.getNode(childrenName.get(0));
            return (V) children.get(key,tree);
            //如果key大于等于节点最右边的key，沿最后一个子节点继续搜索
        }else if (key.compareTo(entries.get(entries.size()-1).getKey()) >= 0) {

            BplusNode children=tree.getNode(childrenName.get(childrenName.size()-1));

            return (V) children.get(key,tree);
            //否则沿比key大的前一个子节点继续搜索
        }else {
            int low = 0, high = entries.size() - 1, mid= 0;
            int comp ;
            while (low <= high) {
                mid = (low + high) / 2;
                comp = entries.get(mid).getKey().compareTo(key);
                if (comp == 0) {

                    BplusNode children=tree.getNode(childrenName.get(mid+1));

                    return (V) children.get(key,tree);
                } else if (comp < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }


            BplusNode children=tree.getNode(childrenName.get(low));

            return (V) children.get(key,tree);
        }
    }

    public void insertOrUpdate(K key, V value, BplusTree<K,V> tree) throws IOException, ClassNotFoundException {
        //如果是叶子节点
        if (isLeaf){
            //不需要分裂，直接插入或更新
            if (contains(key) != -1 || entries.size() < tree.getOrder()){
                insertOrUpdateFile(key, value,tree);
                if(tree.getHeight() == 0){
                    tree.setHeight(1);
                }
                return ;
            }
            //需要分裂
            //分裂成左右两个节点
            String l=UUID.randomUUID().toString();
            String r=UUID.randomUUID().toString();
            BplusNode<K,V> left = new BplusNode<K,V>(true,l,l);
            BplusNode<K,V> right = new BplusNode<K,V>(true,r,r);
            //设置链接
            if (previousName != null){

                BplusNode previous=tree.getNode(previousName);

                previous.nextName = left.name;
                left.previousName = previous.name;

                save(previous,tree);

            }
            if (nextName != null) {
                BplusNode next=tree.getNode(nextName);
                next.previousName = right.name;
                right.nextName = next.name;

                save(next,tree);

            }
            if (previousName == null){
                tree.setHead(left);
            }



            left.nextName = right.name;
            right.previousName = left.name;
            previousName = null;
            nextName = null;
            save(this,tree);
            save(left,tree);
            save(right,tree);

            //复制原节点关键字到分裂出来的新节点
            copy2Nodes(key, value, left, right, tree);

            //如果不是根节点
            if (parentName != null) {
                //调整父子节点关系

                BplusNode parent=tree.getNode(parentName);

                int index = parent.childrenName.indexOf(this.name);

                parent.childrenName.remove(this.name);


                left.parentName = parent.name;
                right.parentName = parent.name;
                parent.childrenName.add(index,left.name);
                parent.childrenName.add(index + 1, right.name);
                parent.entries.add(index,right.entries.get(0));
                entries = null; //删除当前节点的关键字信息
                childrenName = null; //删除当前节点的孩子节点引用

                //父节点插入或更新关键字
                parent.updateInsert(tree);

                parentName = null; //删除当前节点的父节点引用
                save(this,tree);
                save(parent,tree);
                //如果是根节点
            }else {
                isRoot = false;
                BplusNode<K,V> parent = new BplusNode<K,V> (false, true,"root","root");
                tree.setRoot(parent);
                left.parentName = parent.name;
                right.parentName = parent.name;
                parent.childrenName.add(left.name);
                parent.childrenName.add(right.name);
                parent.entries.add(right.entries.get(0));
                entries = null;
                childrenName = null;
                save(parent,tree);
                save(left,tree);
                save(right,tree);
            }
            return ;

        }
        //如果不是叶子节点
        //如果key小于等于节点最左边的key，沿第一个子节点继续搜索
        if (key.compareTo(entries.get(0).getKey()) < 0) {
            BplusNode children=tree.getNode(childrenName.get(0));
            children.insertOrUpdate(key, value, tree);
            save(children,tree);
            //如果key大于节点最右边的key，沿最后一个子节点继续搜索
        }else if (key.compareTo(entries.get(entries.size()-1).getKey()) >= 0) {
            BplusNode children=tree.getNode(childrenName.get(childrenName.size()-1));
            children.insertOrUpdate(key, value, tree);
            save(children,tree);
            //否则沿比key大的前一个子节点继续搜索
        }else {
            int low = 0, high = entries.size() - 1, mid= 0;
            int comp ;
            while (low <= high) {
                mid = (low + high) / 2;
                comp = entries.get(mid).getKey().compareTo(key);
                if (comp == 0) {
                    BplusNode children=tree.getNode(childrenName.get(mid+1));
                    children.insertOrUpdate(key, value, tree);
                    save(children,tree);
                    break;
                } else if (comp < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            if(low>high){
                BplusNode children=tree.getNode(childrenName.get(low));
                children.insertOrUpdate(key, value, tree);
                save(children,tree);
            }
        }





    }

    private void copy2Nodes(K key, V value, BplusNode<K,V> left,
                            BplusNode<K,V> right,BplusTree<K,V> tree) throws IOException {
        //左右两个节点关键字长度
        int leftSize = (tree.getOrder() + 1) / 2 + (tree.getOrder() + 1) % 2;
        boolean b = false;//用于记录新元素是否已经被插入
        for (int i = 0; i < entries.size(); i++) {
            if(leftSize !=0){
                leftSize --;
                if(!b&&entries.get(i).getKey().compareTo(key) > 0){
                    left.entries.add(new SimpleEntry<K, V>(key, value));
                    b = true;
                    i--;
                }else {
                    left.entries.add(entries.get(i));
                }
            }else {
                if(!b&&entries.get(i).getKey().compareTo(key) > 0){
                    right.entries.add(new SimpleEntry<K, V>(key, value));
                    b = true;
                    i--;
                }else {
                    right.entries.add(entries.get(i));
                }
            }
        }
        if(!b){
            right.entries.add(new SimpleEntry<K, V>(key, value));
        }

        save(right,tree);
        save(left,tree);

    }

    /** 插入节点后中间节点的更新 */
    protected void updateInsert(BplusTree<K,V> tree) throws IOException, ClassNotFoundException {

        //如果子节点数超出阶数，则需要分裂该节点

        if (childrenName.size() > tree.getOrder()) {
            //分裂成左右两个节点
            String l=UUID.randomUUID().toString();
            String r=UUID.randomUUID().toString();
            BplusNode<K, V> left = new BplusNode<K, V>(false,l,l);
            BplusNode<K, V> right = new BplusNode<K, V>(false,r,r);
            //左右两个节点子节点的长度
            int leftSize = (tree.getOrder() + 1) / 2 + (tree.getOrder() + 1) % 2;
            int rightSize = (tree.getOrder() + 1) / 2;
            //复制子节点到分裂出来的新节点，并更新关键字
            for (int i = 0; i < leftSize; i++){
                left.childrenName.add(childrenName.get(i));
                BplusNode children=tree.getNode(childrenName.get(i));
                children.parentName = left.name;
                save(children,tree);
            }
            for (int i = 0; i < rightSize; i++){

                right.childrenName.add(childrenName.get(leftSize + i));
                BplusNode children=tree.getNode(childrenName.get(leftSize + i));
                children.parentName = right.name;
                save(children,tree);
            }
            for (int i = 0; i < leftSize - 1; i++) {
                left.entries.add(entries.get(i));
            }
            for (int i = 0; i < rightSize - 1; i++) {
                right.entries.add(entries.get(leftSize + i));
            }

            //如果不是根节点
            if (parentName != null) {
                //调整父子节点关系
                BplusNode parent=tree.getNode(parentName);
                int index = parent.childrenName.indexOf(this.name);
                parent.childrenName.remove(this.name);
                left.parentName = parent.name;
                right.parentName = parent.name;
                parent.childrenName.add(index,left.name);
                parent.childrenName.add(index + 1, right.name);
                parent.entries.add(index,entries.get(leftSize - 1));
                entries = null;
                childrenName = null;

                //父节点更新关键字
                parent.updateInsert(tree);
                parentName = null;
                save(parent,tree);
                //如果是根节点
            }else {
                isRoot = false;
                String name=UUID.randomUUID().toString();
                this.name=name;
                this.path=this.name;
                BplusNode<K, V> parent = new BplusNode<K, V>(false, true,"root","root");
                tree.setRoot(parent);
                tree.setHeight(tree.getHeight() + 1);
                left.parentName = parent.name;
                right.parentName = parent.name;
                parent.childrenName.add(left.name);
                parent.childrenName.add(right.name);
                parent.entries.add(entries.get(leftSize - 1));
                entries = null;
                childrenName = null;
                save(parent,tree);
            }

            save(this,tree);
            save(left,tree);
            save(right,tree);

        }
    }

    /** 删除节点后中间节点的更新*/
    protected void updateRemove(BplusTree<K,V> tree) throws IOException, ClassNotFoundException {

        // 如果子节点数小于M / 2或者小于2，则需要合并节点
        if (childrenName.size() < tree.getOrder() / 2 || childrenName.size() < 2) {
            if (isRoot) {
                // 如果是根节点并且子节点数大于等于2，OK
                if (childrenName.size() >= 2) return;
                // 否则与子节点合并
                BplusNode one=tree.getNode(childrenName.get(0));
                BplusNode<K, V> root = one;
                tree.setRoot(root);
                tree.setHeight(tree.getHeight() - 1);
                root.parentName = null;
                root.isRoot = true;
                entries = null;
                childrenName = null;
                root.name="root";
                root.path="root";
                save(this,tree);
                save(root,tree);
                return ;
            }
            //计算前后节点
            BplusNode parent=tree.getNode(parentName);
            int currIdx = parent.childrenName.indexOf(this.name);
            int prevIdx = currIdx - 1;
            int nextIdx = currIdx + 1;
            BplusNode<K, V> previous = null, next = null;
            BplusNode children=null;
            if (prevIdx >= 0) {
                children=tree.getNode((String) parent.childrenName.get(prevIdx));
                previousName = children.name;
            }
            if (nextIdx < parent.childrenName.size()) {
                children=tree.getNode((String) parent.childrenName.get(nextIdx));
                nextName = children.name;
            }

             if (previousName!=null){
                 previous=tree.getNode(previousName);
             }

            // 如果前节点子节点数大于M / 2并且大于2，则从其处借补
            if (previousName != null
                    && previous.childrenName.size() > tree.getOrder() / 2
                    && previous.childrenName.size() > 2) {
                //前叶子节点末尾节点添加到首位
                int idx = previous.childrenName.size() - 1;

                BplusNode<K, V> borrow = tree.getNode(previous.childrenName.get(idx));
                previous.childrenName.remove(idx);
                borrow.parentName = this.name;
                childrenName.add(0, borrow.name);
                int preIndex = parent.childrenName.indexOf(previous.name);

                entries.add(0, (Entry<K, V>) parent.entries.get(preIndex));
                parent.entries.set(preIndex, previous.entries.remove(idx - 1));

                save(this,tree);
                save(parent,tree);
                save(previous,tree);
                save(borrow,tree);

                return ;
            }
             if (nextName!=null){
                next=tree.getNode(nextName);
             }
            // 如果后节点子节点数大于M / 2并且大于2，则从其处借补
            if (nextName != null
                    && next.childrenName.size() > tree.getOrder() / 2
                    && next.childrenName.size() > 2) {
                //后叶子节点首位添加到末尾
                BplusNode<K, V> borrow = tree.getNode(next.childrenName.get(0));
                next.childrenName.remove(0);
                borrow.parentName = this.name;
                childrenName.add(borrow.name);
                int preIndex = parent.childrenName.indexOf(this.name);
                entries.add((Entry<K, V>) parent.entries.get(preIndex));
                parent.entries.set(preIndex, next.entries.remove(0));

                save(this,tree);
                save(borrow,tree);
                save(parent,tree);
                save(next,tree);


                return ;
            }

            // 同前面节点合并

            if (previousName!=null){
                previous=tree.getNode(previousName);
            }

            if (previousName != null
                    && (previous.childrenName.size() <= tree.getOrder() / 2
                    || previous.childrenName.size() <= 2)) {
                for (int i = 0; i < childrenName.size(); i++) {
                    previous.childrenName.add(childrenName.get(i));
                }
                for(int i = 0; i < previous.childrenName.size();i++){
                    tree.getNode(previous.childrenName.get(i)).parentName = this.name;
                }
                int indexPre = parent.childrenName.indexOf(previous.name);
                previous.entries.add((Entry<K, V>) parent.entries.get(indexPre));
                for (int i = 0; i < entries.size(); i++) {
                    previous.entries.add(entries.get(i));
                }
                childrenName = previous.childrenName;
                entries = previous.entries;

                //更新父节点的关键字列表
                parent.childrenName.remove(previousName);
                previous.parentName = null;
                previous.childrenName = null;
                previous.entries = null;
                parent.entries.remove(parent.childrenName.indexOf(this.name));

                save(this,tree);
                save(previous,tree);


                if((!parent.isRoot
                        && (parent.childrenName.size() >= tree.getOrder() / 2
                        && parent.childrenName.size() >= 2))
                        ||parent.isRoot && parent.childrenName.size() >= 2){
                    save(parent,tree);
                    return ;
                }
                parent.updateRemove(tree);
                save(parent,tree);
                return ;
            }

            // 同后面节点合并
            if (nextName!=null){
                next=tree.getNode(nextName);
            }

            if (nextName != null
                    && (next.childrenName.size() <= tree.getOrder() / 2
                    || next.childrenName.size() <= 2)) {
                for (int i = 0; i < next.childrenName.size(); i++) {
                    BplusNode<K, V> child = tree.getNode(next.childrenName.get(i));
                    childrenName.add(child.name);
                    child.parentName = this.name;
                    save(child,tree);
                }
                int index = parent.childrenName.indexOf(this.name);
                entries.add((Entry<K, V>) parent.entries.get(index));
                for (int i = 0; i < next.entries.size(); i++) {
                    entries.add(next.entries.get(i));
                }
                parent.childrenName.remove(next.name);
                next.parentName = null;
                next.childrenName = null;
                next.entries = null;
                parent.entries.remove(parent.childrenName.indexOf(this.name));

                save(this,tree);
                save(next,tree);

                if((!parent.isRoot && (parent.childrenName.size() >= tree.getOrder() / 2
                        && parent.childrenName.size() >= 2))
                        ||parent.isRoot && parent.childrenName.size() >= 2){
                    save(parent,tree);
                    return ;
                }
                parent.updateRemove(tree);
                save(parent,tree);
                return ;
            }
        }
    }

    public V remove(K key, BplusTree<K,V> tree) throws IOException, ClassNotFoundException {
        //如果是叶子节点
        if (isLeaf){
            //如果不包含该关键字，则直接返回
            if (contains(key) == -1){
                return null;
            }
            //如果既是叶子节点又是根节点，直接删除
            if (isRoot) {
                if(entries.size() == 1){
                    tree.setHeight(0);
                }
                return removeNode(key,tree);
            }
            //如果关键字数大于M / 2，直接删除
            if (entries.size() > tree.getOrder() / 2 && entries.size() > 2) {
                return removeNode(key,tree);
            }
            //如果自身关键字数小于M / 2，并且前节点关键字数大于M / 2，则从其处借补
            BplusNode previous=null;
            if (previousName!=null){
                previous=tree.getNode(previousName);
            }

            if (previousName != null &&
                    previous.parentName .equals(parentName)
                    && previous.entries.size() > tree.getOrder() / 2
                    && previous.entries.size() > 2 ) {
                //添加到首位
                int size = previous.entries.size();
                entries.add(0, (Entry<K, V>) previous.entries.remove(size - 1));
                BplusNode parent=tree.getNode(parentName);
                int index = parent.childrenName.indexOf(previous.name);
                parent.entries.set(index, entries.get(0));

                save(this,tree);
                save(parent,tree);
                save(previous,tree);

                return removeNode(key,tree);
            }
            //如果自身关键字数小于M / 2，并且后节点关键字数大于M / 2，则从其处借补
            BplusNode next=null;
            if (nextName!=null){
                next=tree.getNode(nextName);
            }

            if (nextName != null
                    && next.parentName .equals(parentName)
                    && next.entries.size() > tree.getOrder() / 2
                    && next.entries.size() > 2) {
                entries.add((Entry<K, V>) next.entries.remove(0));

                BplusNode parent=tree.getNode(parentName);
                int index = parent.childrenName.indexOf(this.name);
                parent.entries.set(index, next.entries.get(0));

                save(this,tree);
                save(next,tree);
                save(parent,tree);

                return removeNode(key,tree);
            }

            if (previousName!=null){
                previous=tree.getNode(previousName);
            }
            //同前面节点合并
            if (previousName != null
                    && previous.parentName .equals(parentName)
                    && (previous.entries.size() <= tree.getOrder() / 2
                    || previous.entries.size() <= 2)) {
                V returnValue =  removeNode(key,tree);
                for (int i = 0; i < entries.size(); i++) {
                    //将当前节点的关键字添加到前节点的末尾
                    previous.entries.add(entries.get(i));
                }
                entries = previous.entries;

                BplusNode parent=tree.getNode(parentName);

                parent.childrenName.remove(previous.name);
                previous.parentName = null;
                previous.entries = null;

                save(this,tree);
                save(parent,tree);
                save(previous,tree);

                //更新链表
                if (previous.previousName != null) {
                    BplusNode<K, V> temp = previous;

                    BplusNode tempPrevious=tree.getNode(temp.previousName);

                    tempPrevious.nextName = this.name;

                    previous.name = temp.previousName;

                    temp.previousName = null;
                    temp.nextName = null;

                    save(previous,tree);
                    save(tempPrevious,tree);

                }else {
                    tree.setHead(this);
                    previous.nextName = null;
                    previousName = null;
                    save(previous,tree);
                    save(this,tree);
                }
                parent.entries.remove(parent.childrenName.indexOf(this.name));


                if((!parent.isRoot && (parent.childrenName.size() >= tree.getOrder() / 2
                        && parent.childrenName.size() >= 2))
                        ||parent.isRoot && parent.childrenName.size() >= 2){

                    save(parent,tree);
                    return returnValue;
                }
                parent.updateRemove(tree);

                save(parent,tree);
                return returnValue;
            }
            //同后面节点合并
            if (nextName!=null){
                next=tree.getNode(nextName);
            }
            if(nextName != null
                    && next.parentName .equals( parentName)
                    && (next.entries.size() <= tree.getOrder() / 2
                    || next.entries.size() <= 2)) {
                V returnValue = removeNode(key,tree);
                for (int i = 0; i < next.entries.size(); i++) {
                    //从首位开始添加到末尾
                    entries.add((Entry<K, V>) next.entries.get(i));
                }
                next.parentName = null;
                next.entries = null;

                BplusNode parent=tree.getNode(parentName);

                parent.childrenName.remove(nextName);
                //更新链表
                if (next.nextName != null) {
                    BplusNode<K, V> temp = next;
                    tree.getNode(temp.nextName).previousName = this.name;

                    save(tree.getNode(temp.nextName),tree);

                    nextName = temp.nextName;

                    temp.previousName = null;
                    temp.nextName = null;

                    save(temp,tree);

                }else {
                    next.previousName = null;
                    nextName = null;
                }
                save(this,tree);
                save(next,tree);
                //更新父节点的关键字列表
                parent.entries.remove(parent.childrenName.indexOf(this.name));
                if((!parent.isRoot && (parent.childrenName.size() >= tree.getOrder() / 2
                        && parent.childrenName.size() >= 2))
                        ||parent.isRoot && parent.childrenName.size() >= 2){
                    save(parent,tree);
                    return returnValue;
                }

                parent.updateRemove(tree);
                save(parent,tree);
                return returnValue;
            }
        }
        /*如果不是叶子节点*/

        //如果key小于等于节点最左边的key，沿第一个子节点继续搜索
        if (key.compareTo(entries.get(0).getKey()) < 0) {
            return (V) tree.getNode(childrenName.get(0)).remove(key, tree);
            //如果key大于节点最右边的key，沿最后一个子节点继续搜索
        }else if (key.compareTo(entries.get(entries.size()-1).getKey()) >= 0) {
            return (V) tree.getNode(childrenName.get(childrenName.size()-1)).remove(key, tree);
            //否则沿比key大的前一个子节点继续搜索
        }else {
            int low = 0, high = entries.size() - 1, mid= 0;
            int comp ;
            while (low <= high) {
                mid = (low + high) / 2;
                comp = entries.get(mid).getKey().compareTo(key);
                if (comp == 0) {
                    return (V) tree.getNode(childrenName.get(mid + 1)).remove(key, tree);
                } else if (comp < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            return (V) tree.getNode(childrenName.get(low)).remove(key, tree);
        }
    }

    /** 判断当前节点是否包含该关键字*/
    protected int contains(K key) {
        int low = 0, high = entries.size() - 1, mid;
        int comp ;
        while (low <= high) {
            mid = (low + high) / 2;
            comp = entries.get(mid).getKey().compareTo(key);
            if (comp == 0) {
                return mid;
            } else if (comp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    /** 插入到当前节点的关键字中*/
    protected void insertOrUpdateFile(K key, V value, BplusTree<K,V> tree) throws IOException {
        //二叉查找，插入
        int low = 0, high = entries.size() - 1, mid;
        int comp ;
        while (low <= high) {
            mid = (low + high) / 2;
            comp = entries.get(mid).getKey().compareTo(key);
            if (comp == 0) {
                entries.get(mid).setValue(value);
                break;
            } else if (comp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        if(low>high){
            entries.add(low, new SimpleEntry<K, V>(key, value));

            save(this,tree);

        }
    }

    /** 删除节点*/
    protected V removeNode(K key,BplusTree tree) throws IOException {
        int low = 0,high = entries.size() -1,mid;
        int comp;
        while(low<= high){
            mid  = (low+high)/2;
            comp = entries.get(mid).getKey().compareTo(key);
            if(comp == 0){
                Object v=entries.remove(mid).getValue();
                save(this,tree);
                return (V) v;
            }else if(comp < 0){
                low = mid + 1;
            }else {
                high = mid - 1;
            }
        }
        return null;
    }
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("isRoot: ");
        sb.append(isRoot);
        sb.append(", ");
        sb.append("isLeaf: ");
        sb.append(isLeaf);
        sb.append(", ");
        sb.append("keys: ");
        for (Entry<K,V> entry : entries){
            sb.append(entry.getKey());
            sb.append(", ");
        }
        sb.append(", ");
        return sb.toString();
    }

    private void save(BplusNode node,BplusTree tree) throws IOException{
        File file=new File(node.path);
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(node);
        oos.close();
        tree.hashMap.put(node.name,node.path);
        tree.tmp.put(node.name,node);

        tree.i++;


    }



}
