ya tengo el readme, como lo subo junto al javadoc y el proyecto en si


# Pi Pattern & Geometry Analyzer

An interactive Java Swing application designed for real-time visual analysis, geometric pattern recognition, and sequence searching (numerical and alphabetical) across the infinite digits of π.

It features a hybrid data engine: it streams gigabyte-scale digit files from disk without memory overhead and seamlessly fallbacks to real-time, arbitrary-precision calculation using a mathematical Spigot algorithm.

---

## Key Features

* **Hybrid Digit Source:**
  * **File Streaming:** Instant lookup in gigabyte-scale `pi_digits.txt` files using `RandomAccessFile` (zero RAM bloat).
  * **Spigot Engine Fallback:** Real-time generation of exact π digits using `BigInteger` precision (Jeremy Gibbons' Unbounded Spigot Algorithm) if no text file is present or when exceeding file bounds.
* **4 Visual Display Modes:**
  * **10-Color Palette:** Maps digits 0-9 to distinct colors for visual geometric pattern identification.
  * **Monochrome (Parity):** Binary black/white representation based on odd/even digit parity.
  * **Numeric Grid:** Clean, standard matrix representation of decimal digits.
  * **English Letters (A-Z):** Maps digit pairs ((d1 × 10 + d2) mod 26) to alphabetic characters to search for text hidden in π.
* **Sequence & Pattern Search:**
  * Search for numerical sequences (e.g., `31415`, `9265`).
  * Search for alphabetic words (e.g., `HELLO`, `PI`) mapped dynamically across digits.
* **Interactive Navigation:** Mouse-wheel scrolling, direct index jump (`Go to #`), and click-and-drag cell range selection.
* **Export to Image:** Render and export selected patterns or regions to high-resolution PNG files.

---

## Architecture & Technical Highlights

| Component | Implementation Details |
| :--- | :--- |
| **Algorithm** | Implements Jeremy Gibbons' Unbounded Spigot Algorithm for π using `BigInteger`. |
| **Memory Management** | Sequential digit caching to prevent re-computation combined with memory-efficient disk streaming. |
| **GUI & Graphics** | Custom Swing `JPanel` double-buffered rendering via `Graphics2D` for high-FPS scrolling. |
| **I/O Strategy** | Direct byte positioning with `RandomAccessFile` for O(1) disk lookups on arbitrary positions. |

---

## Prerequisites

* **Java Development Kit (JDK):** Version 17 or higher.
* **Build System / IDE:** Any Java IDE (IntelliJ IDEA, Eclipse, VS Code) or command-line compilation tools (`javac`/`java`).

---

## Installation & Execution

1. **Clone the repository:**
```
git clone [https://github.com/josegonzalezal/PiPatternAnalyzer.git](https://github.com/josegonzalezal/PiPatternAnalyzer.git)
cd PiPatternAnalyzer
```
