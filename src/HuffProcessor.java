import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 *
 * @author Ow	en Astrachan
 *
 * Revise
 */

public class HuffProcessor {

	private class HuffNode implements Comparable<HuffNode> {
		HuffNode left;
		HuffNode right;
		int value;
		int weight;

		public HuffNode(int val, int count) {
			value = val;
			weight = count;
		}
		public HuffNode(int val, int count, HuffNode ltree, HuffNode rtree) {
			value = val;
			weight = count;
			left = ltree;
			right = rtree;
		}

		public int compareTo(HuffNode o) {
			return weight - o.weight;
		}
	}

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD);
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private boolean myDebugging = false;

	public HuffProcessor() {
		this(false);
	}
	public HuffProcessor(boolean debug) {
		myDebugging = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file
	 *
	 */
	public void compress(BitInputStream in, BitOutputStream out) {
		int[] counts = getCounts(in);
		HuffNode root = makeTree(counts);
		in.reset();
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTree(root, out);
		String[] encodings = new String[ALPH_SIZE + 1];
		makeEncodings(root, " ", encodings);
		while (true) {
			int a = in.readBits(BITS_PER_WORD);
			if (a == -1){
				String code = encodings[PSEUDO_EOF];
				break;
			}
			String code = encodings[a];
			out.writeBits(code.length(), Integer.parseInt(code, 2));
		}

		out.close();
	}


	private void writeTree(HuffNode root, BitOutputStream out) {
		if (root == null){return;}
		if (root.left == null && root.right == null){out.writeBits(BITS_PER_WORD+ 2, 1 + root.value);}
		else{out.writeBits(1, 0);
			writeTree(root.left, out);
			writeTree(root.right,out);
	}}
	private void makeEncodings(HuffNode root,String path, String[] encodings) {
		if (root == null) {return;}
		if(root.left == null && root.right == null){
			encodings[root.value] = path;
		}
		if (root.left != null && root.right == null){
			makeEncodings(root.left, path + 0, encodings);
			makeEncodings(root.right, path + 1, encodings);}
	}

	private HuffNode makeTree(int[] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for(int k  = 0; k < ALPH_SIZE; k+=1){
			if (counts[k] > 0){
			pq.add(new HuffNode(k, counts[k],null, null));
		}
	}
		while (pq.size() > 1){
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode( 0, left.weight + right.weight, left, right );
			pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}

	private int[] getCounts(BitInputStream in) {
		int[] counts = new int[ALPH_SIZE];
		while (true){
		int a = in.readBits(BITS_PER_WORD);
		if (a == -1){break;}
		counts[a] += 1;}
		 return counts;

	}
	

	public void decompress(BitInputStream in, BitOutputStream out){
		int huffnum = in.readBits(BITS_PER_INT);
		if (huffnum != HUFF_TREE) {
			throw new HuffException("Invalid magic number" + huffnum);
		}

		HuffNode root = readTree(in);
		HuffNode current = root;

		while (true) {
			int bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("invalid bits found"); }
			if (bits == 0) {
				current = current.left;
			}
			else {
				current = current.right;
			}
			if (current.left == null && current.right == null) {
				if (current.value == PSEUDO_EOF) {
					break;
				} else {
					out.writeBits(BITS_PER_WORD, current.value);
					current = root;
				}
			}
		}
		out.close();
	}
	private HuffNode readTree(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1) {throw new HuffException("No bits found");}
		if (bit == 0) {
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);
			return new HuffNode(0,0,left,right);
		}
		else {
			int value = in.readBits(BITS_PER_WORD + 1);
			return new HuffNode(value,0,null,null);
		}
	}}