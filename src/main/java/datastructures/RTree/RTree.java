package datastructures.RTree;

import datastructures.ElementWithCoordinates;
import datastructures.LinkedList.LinkedList;
import models.Post;

import java.util.Arrays;

public class RTree {

    public static String DATA_STRUCTURE_NAME = "R-Tree";

    // TODO: Change "MAX_ITEMS" to a more meaningful name
    public static final int MAX_ITEMS = 3;
    public static final int MIN_ITEMS = MAX_ITEMS /2 + MAX_ITEMS %2;

    private Node root;

    public RTree() {
        root = new LeafNode(null);
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    public void addPost(Post post) {
        addPost(post, root);
    }

    public static RTree getTestRTree() {
        RTree rTree = new RTree();
        rTree.root = new InternalNode(null);
        rTree.root.length = 2;
        InternalNode i1 = new InternalNode(rTree.root);
        i1.setStart(new double[] {6, 6});
        i1.setEnd(new double[] {7, 7});
        LeafNode l1 = new LeafNode(rTree.root);
        ((InternalNode) rTree.root).child[0] = l1;
        LeafNode l2 = new LeafNode(i1);
        ((InternalNode) rTree.root).child[1] = i1;
        i1.child[0] = l2;

        l1.addPost(new Post(1, new double[] {0,0}));
        l1.addPost(new Post(2, new double[] {5,5}));
        l2.addPost(new Post(3, new double[] {6,6}));
        l2.addPost(new Post(4, new double[] {7,7}));

        rTree.root.start[0] = 0;
        rTree.root.start[1] = 0;
        rTree.root.end[0] = 7;
        rTree.root.end[1] = 7;

        //l1.addPost(new Post(5, new double[]{0,5}));
        //l1.addPost(new Post(6, new double[]{5,0}));
        //l1.addPost(new Post(7, new double[]{5,0}));
        return rTree;
    }

    public void findCandidates(double[] postLocation, Node root, LinkedList linkedList) {
        if (postInTheRegion(root.getStart(), root.getEnd(), postLocation)) {
            if (root instanceof LeafNode) {
                linkedList.add(root);
            } else {
                for (Node n : ((InternalNode) root).getChild()) {
                    if(n == null) continue;
                    findCandidates(postLocation, n, linkedList);
                }
            }
        }
    }

    public void addPost(Post post, Node nextNode) {
        // Life saver: http://www.mathcs.emory.edu/~cheung/Courses/554/Syllabus/3-index/R-tree.html
        double[] postLocation = post.getLocation();

        LinkedList<Node> candidates = new LinkedList<>();
        findCandidates(postLocation, nextNode, candidates);

        if (nextNode instanceof InternalNode) {
            Node[] child = ((InternalNode) nextNode).getChild();

            // Provem a insertar el node a tots els child
            for (int i = 0; i < nextNode.length; i++) {
                Node n = child[i];
                if (postInTheRegion(n.getStart(), n.getEnd(), postLocation)) {
                    addPost(post, n);
                    return;
                }
            }

            // Si arribem aqui es que no hi ha cap node que el pugui agafar (perque no esta "inTheRegion")...
            // Ara hem de trobar el bounding rectangle a "nextNode" que augmenti area minima al insertar el post
            Node minimumAreaNode = child[0];
            double minimumAreaIncrease = calculaIncrement(child[0].start, child[0].end, post.getLocation());

            for (int i = 1; i < nextNode.length; i++) {
                Node n = child[i];
                double a = calculaIncrement(n.start, n.end, post.getLocation());
                if (a < minimumAreaIncrease) {
                    minimumAreaNode = n;
                    minimumAreaIncrease = a;
                }
            }

            // Quan trobem el que augmentaria minim l'area, fem crida recursiva per insertar el post a aquell node
            addPost(post, minimumAreaNode);
        } else {
            if (nextNode.isFull()) {
                split((LeafNode) nextNode, post);
            } else {
                ((LeafNode) nextNode).addPost(post);
            }
        }
    }

    public void split (LeafNode n, Post p) {
        Post[] postsToAdd = Arrays.copyOf(n.getPosts(), n.getLength() + 1);
        postsToAdd[n.getLength()] = p;

        // Troba els 2 posts mes allunyats entre ells:
        Post[] furthestPosts = findFurthestPosts(postsToAdd);

        // Ara ja tenim els 2 posts mes allunyats entre ells.
        // Creem 2 regions i 2 leafNodes i inserim un post a cada leafNode:
        Post[] tmpPosts = n.getPosts();
        tmpPosts[0] = furthestPosts[0];
        n.setPosts(tmpPosts);
        n.setLength(1);
        LeafNode newLeafNode = new LeafNode(n.getParent());
        newLeafNode.addPost(furthestPosts[1]);

        // Ara que ja tenim els 2 posts en les seves regions, s'han d'afegir la resta de posts entre la R1 i la R2:
        addRemainingPosts(n, newLeafNode, postsToAdd, furthestPosts);

        ((InternalNode) n.getParent()).addChild(newLeafNode);
    }

    private static double calculaIncrement(double[] start, double[] end, double[] location) {
        double[] startClone = start.clone();
        double[] endClone = end.clone();
        double areaInicial = (endClone[0] - startClone[0]) * (endClone[1] - startClone[1]);

        if (location[0] > endClone[0]) {
            endClone[0] = location[0];
        }

        if (location[1] > endClone[1]) {
            endClone[1] = location[1];
        }

        if (location[0] < startClone[0]) {
            startClone[0] = location[0];
        }

        if (location[1] < startClone[1]) {
            startClone[1] = location[1];
        }

        double areaFinal = (endClone[0] - startClone[0]) * (endClone[1] - startClone[1]);

        return areaFinal - areaInicial;
    }

    private static double calculaArea(double[] p1, double[] p2) {
        return (p2[0] - p1[0]) * (p2[1] - p1[1]);
    }

    public Post getPost(double[] location, Node nextNode) {
        if (nextNode != null && postInTheRegion(nextNode.getStart(), nextNode.getEnd(), location)) {
            if (nextNode instanceof InternalNode) {
                Node[] child = ((InternalNode) nextNode).getChild();

                for (Node n : child) {
                    Post tmp = getPost(location, n);

                    if (tmp == null) {
                        continue;
                    }

                    return tmp;
                }
            } else if (nextNode instanceof LeafNode){
                Post[] posts = ((LeafNode) nextNode).getPosts();

                for (Post p : posts) {
                    if (p != null && Arrays.equals(p.getLocation(), location)) {
                        return p;
                    }
                }
            }
        }

        return null;
    }

    // Returns the post on that specific location
    public Post getPost(double[] location) {
        return getPost(location, root);
    }

    // Returns the posts inside that region
    public LinkedList<Post> getPosts(double[] start, double[] end, Node nextNode) {
        LinkedList<Post> posts = new LinkedList<>();
        Node[] child = ((InternalNode) nextNode).getChild();

        for(Node n : child) {
            if(n instanceof InternalNode && regionIntersectsRegion(start, end, n.getStart(), n.getEnd())) {
                LinkedList<Post> aux = getPosts(start, end, n);
                Post[] auxArr = aux.toArray(new Post[aux.getSize()]);
                for(Post p: auxArr) {
                    posts.add(p);
                }
            }
            if(n instanceof LeafNode) {
                for(Post p: ((LeafNode) n).getPosts()) {
                    if(p != null && postInTheRegion(start, end, p.getLocation())) {
                        posts.add(p);
                    }
                }
            }
        }
        return posts;
    }

    //Mira si hi ha interseccio entre dues regions:
    private boolean regionIntersectsRegion(double[] start, double[] end, double[] startN, double[] endN) {
        return (startN[0] > start[0] || endN[0] < end[0] || startN[1] > start[1] || endN[1] < end[1]);
    }

    //Remove post by reference:
    public void removePost(Post post, Node nextNode) {
        if(nextNode instanceof InternalNode) {
            Node[] child = ((InternalNode) nextNode).getChild();
            for(Node n : child) {
                if(n instanceof LeafNode) {
                    ((LeafNode) n).removePost(post);
                    ((LeafNode) n).findNewLimits();
                } else if (n instanceof InternalNode) {
                    //Mira si els punts estan dins la regio:
                    if(postInTheRegion(n.getStart(), n.getEnd(), post.getLocation())) {
                        removePost(post, n);
                    }
                }
            }
        } else {
            ((LeafNode) nextNode).removePost(post);
            ((LeafNode) nextNode).findNewLimits();
        }
    }

    //Funció que troba els nous límits de tots els nodes interns fins arribar a l'arrel:
    private void findNewLimits(Node n) {

    }

    //Remove post by location:
    public void removePost(double[] postLocation, Node nextNode) {
        if(nextNode instanceof InternalNode) {
            Node[] child = ((InternalNode) nextNode).getChild();
            for(Node n : child) {
                if(n instanceof LeafNode) {
                    ((LeafNode) n).removePost(postLocation);
                } else if (n instanceof InternalNode) {
                    //Mira si els punts estan dins la regio:
                    if(postInTheRegion(n.getStart(), n.getEnd(), postLocation)) {
                        removePost(postLocation, n);
                    }
                }
            }
        } else {
            ((LeafNode) nextNode).removePost(postLocation);
            ((LeafNode) nextNode).findNewLimits();
        }
    }

    private static boolean postInTheRegion(double[] start, double[] end, double[] location) {
        //Mirem si la location esta dins de les x:
        if(start[0] <= location[0] && start[1] <= location[1]) {
            //Mirem si la location esta dins de les y:
            if(location[0] <= end[0] && location[1] <= end[1]) {
                return true;
            }
        }

        return false;
    }

    public void addRemainingPosts(LeafNode l1, LeafNode l2, Post[] posts, Post[] furthestPosts) {
        int postsToAdd = posts.length - 2;
        for(Post p: posts) {
            if(p != furthestPosts[0] && p != furthestPosts[1]) {
                if(MIN_ITEMS - l1.getLength() == postsToAdd) {
                    l1.addPost(p);
                } else if(MIN_ITEMS - l2.getLength() == postsToAdd) {
                    l2.addPost(p);
                } else {
                    if(findNearestRegion(l1, l2, p)) {
                        l1.addPost(p);
                    } else {
                        l2.addPost(p);
                    }
                }
                postsToAdd--;
            }
        }
    }

    //Mira a quina de les dues regions hi hauria increment d'area mes petit per a inserir el post en aquella regio:
    public boolean findNearestRegion(LeafNode leaf1, LeafNode leaf2, Post p) {
        double inc1 = calculaIncrement(leaf1.getStart(), leaf1.getEnd(), p.getLocation());
        double inc2 = calculaIncrement(leaf2.getStart(), leaf2.getEnd(), p.getLocation());
        return inc1 > inc2;
    }


    // Retorna els dos posts mes allunyats entre ells:
    public Post[] findFurthestPosts(Post[] postsArr) {
        double max = 0;
        Post[] posts = new Post[2];
        for(int i = 0; i < postsArr.length; i++) {
            for(int j = i + 1; j < postsArr.length; j++) {
                double dist = calculateDistance(postsArr[i], postsArr[j]);
                if(dist > max) {
                    max = dist;
                    posts[0] = postsArr[i];
                    posts[1] = postsArr[j];
                }
            }
        }
        return posts;
    }

    //Retorna la distancia al quadrat que hi ha entre post1 i post2:
    public double calculateDistance(Post post1, Post post2) {
        double[] locationP1 = post1.getLocation();
        double[] locationP2 = post2.getLocation();
        return (Math.pow(locationP1[0] - locationP2[0], 2) + Math.pow(locationP1[1] - locationP2[1], 2));
    }

}