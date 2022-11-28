package edu.ktu.ds.lab3.utils;

import java.util.Arrays;

import static java.util.Objects.hash;

/**
 * Porų ("maping'ų") raktas-reikšmė objektų kolekcijos - atvaizdžio realizacija
 * maišos lentele, kolizijas sprendžiant atskirų grandinėlių (angl. separate
 * chaining) metodu. Neužmirškite, jei poros raktas - nuosavos klasės objektas,
 * pvz. klasės Car objektas, klasėje būtina perdengti metodus equals(Object o)
 * ir hashCode().
 *
 * @param <K> atvaizdžio raktas
 * @param <V> atvaizdžio reikšmė
 * @author darius.matulis@ktu.lt
 * @Užduotis Peržiūrėkite ir išsiaiškinkite pateiktus metodus.
 */
public class HashMap<K, V> implements EvaluableMap<K, V> {

    public static final int DEFAULT_INITIAL_CAPACITY = 8;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    public static final HashManager.HashType DEFAULT_HASH_TYPE = HashManager.HashType.DIVISION;

    // Maišos lentelė
    protected Node<K, V>[] table;
    // Lentelėje esančių raktas-reikšmė porų kiekis
    protected int size = 0;
    // Apkrovimo faktorius
    protected float loadFactor;
    // Maišos metodas
    protected HashManager.HashType ht;
    //--------------------------------------------------------------------------
    //  Maišos lentelės įvertinimo parametrai
    //--------------------------------------------------------------------------
    // Maksimalus suformuotos maišos lentelės grandinėlės ilgis
    protected int maxChainSize = 0;
    // Permaišymų kiekis
    protected int rehashesCounter = 0;
    // Paskutinės patalpintos poros grandinėlės indeksas maišos lentelėje
    protected int lastUpdatedChain = 0;
    // Lentelės grandinėlių skaičius     
    protected int chainsCounter = 0;

    /* Klasėje sukurti 4 perkloti konstruktoriai, nustatantys atskirus maišos
     * lentelės parametrus. Jei kuris nors parametras nėra nustatomas -
     * priskiriama standartinė reikšmė.
     */
    public HashMap() {
        this(DEFAULT_HASH_TYPE);
    }

    public HashMap(HashManager.HashType ht) {
        this(DEFAULT_INITIAL_CAPACITY, ht);
    }

    public HashMap(int initialCapacity, HashManager.HashType ht) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, ht);
    }

    public HashMap(float loadFactor, HashManager.HashType ht) {
        this(DEFAULT_INITIAL_CAPACITY, loadFactor, ht);
    }

    public HashMap(int initialCapacity, float loadFactor, HashManager.HashType ht) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }

        if ((loadFactor <= 0.0) || (loadFactor > 1.0)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }

        this.table = new Node[initialCapacity];
        this.loadFactor = loadFactor;
        this.ht = ht;
    }

    /**
     * Patikrinama ar atvaizdis yra tuščias.
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Grąžinamas atvaizdyje esančių porų kiekis.
     *
     * @return Grąžinamas atvaizdyje esančių porų kiekis.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Išvalomas atvaizdis.
     */
    @Override
    public void clear() {
        Arrays.fill(table, null);
        size = 0;
        lastUpdatedChain = 0;
        maxChainSize = 0;
        rehashesCounter = 0;
        chainsCounter = 0;
    }

    /**
     * Patikrinama ar pora egzistuoja atvaizdyje.
     *
     * @param key raktas.
     * @return Patikrinama ar pora egzistuoja atvaizdyje.
     */
    @Override
    public boolean contains(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key is null in contains(K key)");
        }

        return get(key) != null;
    }

    /**
     * Atvaizdis papildomas nauja pora.
     *
     * @param key   raktas,
     * @param value reikšmė.
     * @return Atvaizdis papildomas nauja pora.
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key or value is null in put(K key, V value)");
        }
        int index = HashManager.hash(key.hashCode(), table.length, ht);
        if (table[index] == null) {
            chainsCounter++;
        }

        Node<K, V> node = getInChain(key, table[index]);
        if (node == null) {
            table[index] = new Node<>(key, value, table[index]);
            size++;

            if (size > table.length * loadFactor) {
                rehash();
            } else {
                lastUpdatedChain = index;
            }
        } else {
            node.value = value;
            lastUpdatedChain = index;
        }

        return value;
    }

    /**
     * Grąžinama atvaizdžio poros reikšmė.
     *
     * @param key raktas.
     * @return Grąžinama atvaizdžio poros reikšmė.
     */
    @Override
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key is null in get(K key)");
        }

        int index = HashManager.hash(key.hashCode(), table.length, ht);
        Node<K, V> node = getInChain(key, table[index]);
        return node == null ? null : node.value;
    }

    public int hash(K key, HashManager.HashType hashType) {
        int h = key.hashCode();
        switch (hashType) {
            case DIVISION:
                return Math.abs(h) % table.length;
            case MULTIPLICATION:
                double k = (Math.sqrt(5) - 1) / 2;
                return (int) (((k * Math.abs(h)) % 1) * table.length);
            case JCF7:
                h ^= (h >>> 20) ^ (h >>> 12);
                h = h ^ (h >>> 7) ^ (h >>> 4);
                return h & (table.length - 1);
            case JCF8:
                h = h ^ (h >>> 16);
                return h & (table.length - 1);
            default:
                return Math.abs(h) % table.length;
        }
    }

    /**
     * Pora pašalinama iš atvaizdžio.
     *
     * @param key Pora pašalinama iš atvaizdžio.
     * @return key raktas.
     */
    @Override
    public V remove(K key) {
//        throw new UnsupportedOperationException("Studentams reikia realizuoti remove(K key)");
        if (key == null) {
            throw new IllegalArgumentException("Key is null in get(K key)");
        }
        V removedValue = null;
        int index = HashManager.hash(key.hashCode(), table.length, ht);
        Node<K, V> node = table[index];
        int chainSize = 0;
        if (node != null) {
            // Removed node if it is head
            if (node.key.equals(key)) {
                removedValue = table[index].value;
                table[index] = node.next;
                lastUpdatedChain = index;
                size--;
            }

            // If chain is empty, update counter
            if (table[index] == null)
                chainsCounter--;

            else {
                // Finds chain size and parent if exists
                Node<K, V> parent = null;
                if (node != null) {
                    for (; node != null; node = node.next) {
                        chainSize++;
                        if (node.next != null && (node.next.key).equals(key)) {
                            parent = node;
                        }
                    }
                }

                // Removes a node in the middle of the chain
                if (parent != null) {
                    removedValue = parent.next.value;
                    parent.next = parent.next.next;
                    lastUpdatedChain = index;
                    chainSize--;
                    size--;
                }
            }

        }
        maxChainSize = Math.max(maxChainSize, chainSize);
        return removedValue;
    }

    /**
     * Permaišymas
     */
    private void rehash() {
        HashMap<K, V> newMap = new HashMap<>(table.length * 2, loadFactor, ht);
        for (int i = 0; i < table.length; i++) {
            while (table[i] != null) {
                newMap.put(table[i].key, table[i].value);
                table[i] = table[i].next;
            }
        }
        table = newMap.table;
        maxChainSize = newMap.maxChainSize;
        chainsCounter = newMap.chainsCounter;
        lastUpdatedChain = newMap.lastUpdatedChain;
        rehashesCounter++;
    }

    /**
     * Paieška vienoje grandinėlėje
     *
     * @param key
     * @param node
     * @return
     */
    private Node<K, V> getInChain(K key, Node<K, V> node) {
        if (key == null) {
            throw new IllegalArgumentException("Key is null in getInChain(K key, Node node)");
        }
        int chainSize = 0;
        for (Node<K, V> n = node; n != null; n = n.next) {
            chainSize++;
            if ((n.key).equals(key)) {
                return n;
            }
        }
        maxChainSize = Math.max(maxChainSize, chainSize + 1);
        return null;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Node<K, V> node : table) {
            if (node != null) {
                for (Node<K, V> n = node; n != null; n = n.next) {
                    result.append(n).append(System.lineSeparator());
                }
            }
        }
        return result.toString();
    }

    public boolean replace(K key, V oldValue, V newValue) {
//        throw new UnsupportedOperationException("Studentams reikia realizuoti replace(K key,  V oldValue, V newValue)");
        if (key == null) {
            throw new IllegalArgumentException("Key is null in get(K key)");
        }

        int index = HashManager.hash(key.hashCode(), table.length, ht);
        Node<K, V> node = getInChain(key, table[index]);
        if (node != null && (oldValue == null || oldValue.equals(node.value))) {
            node.value = newValue;
            lastUpdatedChain = index;
            return true;
        }
        return false;
    }

    public boolean containsValue(Object value) {
//        throw new UnsupportedOperationException("Studentams reikia realizuoti containsValue(Object value)");
        for (int chainIndex = 0; chainIndex < table.length; chainIndex++) {
            Node<K, V> head = table[chainIndex];
            for (Node<K, V> node = head; node != null; node = node.next)
                if (node.value.equals(value))
                    return true;
        }
        return false;
    }

    public V replace(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key or value is null in replace(Key key, Value value)");
        }

        int index = hash(key, ht);
        if (table[index] == null) {
            chainsCounter++;
        }

        Node<K, V> node = getInChain(key, table[index]);

        if (node == null) {
            table[index] = new Node<>(key, value, table[index]);
            size++;

            if (size > table.length * loadFactor) {
                rehash();
            } else {
                lastUpdatedChain = index;
            }
            return null;
        } else {
            V oldValue = node.value;
            node.value = value;
            lastUpdatedChain = index;
            return oldValue;
        }
    }

    /**
     * Grąžina maksimalų grandinėlės ilgį.
     *
     * @return Maksimalus grandinėlės ilgis.
     */
    @Override
    public int getMaxChainSize() {
        return maxChainSize;
    }

    /**
     * Grąžina formuojant maišos lentelę įvykusių permaišymų kiekį.
     *
     * @return Permaišymų kiekis.
     */
    @Override
    public int getRehashesCounter() {
        return rehashesCounter;
    }

    /**
     * Grąžina maišos lentelės talpą.
     *
     * @return Maišos lentelės talpa.
     */
    @Override
    public int getTableCapacity() {
        return table.length;
    }

    /**
     * Grąžina paskutinės papildytos grandinėlės indeksą.
     *
     * @return Paskutinės papildytos grandinėlės indeksas.
     */
    @Override
    public int getLastUpdated() {
        return lastUpdatedChain;
    }

    /**
     * Grąžina grandinėlių kiekį.
     *
     * @return Grandinėlių kiekis.
     */
    @Override
    public int getNumberOfOccupied() {
        return chainsCounter;
    }

    protected static class Node<K, V> {

        // Raktas        
        protected K key;
        // Reikšmė
        protected V value;
        // Rodyklė į sekantį grandinėlės mazgą
        protected Node<K, V> next;

        protected Node() {
        }

        protected Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}
