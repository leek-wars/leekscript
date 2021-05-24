/*
 * Commentaire large
 */

/* Commentaire /* Imbriqué */ */

/* Commentaire commenté
// */
*/

// Déclaration de variables

let e
let b = 2
let c, d = "salut"
let k, f = 12, 5 // Commentaire à la fin
let g, h, i = 12
global super


// Points virgules
var aa = 2;

// Nombres à virgule

__debug(12.56 - 2)
12.4
100.0001


// Chaines

let a = '', b = ""
a = 'bonjour ça va ?'
'toto'
b = "double quotes"
let cc = " 'salut', 'yeah' "


// Tableaux

let a1 = []
let a2 = [1, 2, 3]
let a3 = [3: 12, "toto": 5]
let b2 = [12, "toto", 1.1, [1, 2, 3]]
let cles = [1: 12, 3: 5, "lama": 177]

__debug(a1)
__debug(a2)
__debug(a3)
__debug(cles)

__debug(a2[1])
__debug(a3['toto'])
__debug(a2[1])
__debug(cles["lama"])

//__debug(b2[toto:5])
//__debug(b2[1:12][5:6])

// Swap

// a <-> b
// a <=> b


// Objets

let array = [1, 2, 3]
let o = {x: 2, y: 13, z: {a: 2}}
let compound = {x: 2, other: {val: 7, tab: [1, 2, 3]}, y: 'zzz'}

__debug(o)
__debug(compound)

__debug(array.size)
__debug(array.0)
__debug(o.size)
__debug(o.x)
__debug(o.z.a)
__debug(compound.other.tab[1])

__debug(2.class)
__debug("salut".class)
__debug({x: 12}.class)
__debug([7,"a"].class)
__debug((x -> x + 5).class)

// Conditions

if array is not null and a is 'z' {
	let c = 13
} else {
	print("yeah")
}

if cc or cles {
	print(a)
}

if c then print(c) else !12 xor -5 end
if a then print(a) else {
	let c = 12
}
if (a != 2) {
	print(a)
}


// Fonctions et Lambdas

let f1 = function(x, y = 2, z = false) return 0 end

let f2 = function(x = "salut", y, z) {
	return x * x
}

let f3 = function(x) return x end
let f4 = -> -> -> 127
let f9 = x,y -> [y: x, x: y]
let f10 = x,y -> z -> [[x,z], [y,z], -> x+y+z]

getLife(12)

[1, 2, 3].sort(a,b -> a < b)
[1, 2, 3].reverse().first().abs() + 5 ^ 2

let f5 = x -> x ^ 2
let g2 = x, y, z -> x * y * z
let h2 = -> "salut"
getCell(-> 12)

let o2 = i,j -> a,b -> i * b + j * a
a -> b -> 2 + a -> c, d -> 5 and print("yeah")
var res = getLife(x -> getID(x))

__debug(f1(1, 2, true))
__debug(f2("salut"))
__debug(f4()()())
__debug(f9(4, "salut"))
let ress = f10(5,6)(100)
__debug(ress)
__debug(ress[2]())


// Accents

let état = "test"
let àôé = 2


// Opérations diverses

2 + 1
'bonjour ça va ?' 5 [1,2] + 4 a = (2 || b) c - 12
let ccc = 16 + b - 5
let ddd = 4 * "salut" / true
let x = 2['salut' - 1] && true || false xor 1

let vv = -15
__debug(vv.abs())


// Signes moins
-1
2 - 5
4 + -5
-10 - -10
- (5 - (-7)) -
-4 +
(-5 - -13) - (-3)


// Opérations sur plusieurs lignes
5
-4

5 -
4

5
- 4

b
=
a
-
5
+
"a"


// Opérateurs préfixes

-2
not false
!"salut"
new a

// Parenthèses

(5) * (4 + 2) - 7 / (4 - (((5))))
print(f(a) + 5)

// Classes

class Combo {

	let score = 12

	let new = function(score = 0) {
		this.score = score
	}

	let b = x -> x + 1

	let update = function() {
		score = score + 1
		return score
	}
	let updateSimple = -> ++score
}

class Test {}

let combo = new Combo(12)
let combo2 = new Combo()
let combo3 = Combo.new(13)
combo3.score = 55
combo3.update()

// Closures

var ext = 55
var fun = function(a) { // [ext]

	let b = 12
	let c = function(d) { // [b, ext]
		return function(e) { [a, b, ext]
			return a + b + d + e + ext
		}
	}
	return c
}

// Opérateurs en mode fonctions

var plus = +
let minus = -
let modulo = (%)
var ternary = (?:)
array.map(+)

var array = [ [true, 10, 12], [false, 5, 7] ]
var res1 = array.map(?:).transform(10, ^, +)

// Valeurs absolues

var res2 = |pos - 12|
var dd = |5 - |pos + 5| |
var dist = |getX(celll) - getY(|cell.pos.x|)|

12.abs()
12.5.abs()

a === b
a !== b
!!a
!!!!!a

a--
b++
--c
++d
a *= b
b -= a
c /= e
g %= r
p ^= z

p = ++(i++ ^ --z % ++z--)--
a --
++ a
a = -b++
12

// Opérateur de fonction

var yoyo = x -> x + " !"
var yo = "salut"
yo ->= yoyo
yo ~= yoyo // yo = "salut !"
yo ()= yoyo


// Boucles for

for ;; {}
for ;; do end
for (;;) {}
for ; ; {
	print("salut")
}

for let i;; {}
for let i = 0;; {}
for let i = 0; i < 10; i++ {}
for let i = 0, j; i < 10; i++ {}
for i,j,let k = 2, l = "salut"; k !== "lama"; l++ {}
for let i,j, let k ;; {}

for let i; i < 10; i++ do
	print(i + 100)
end

// Foreach

for i in array {}
for i,j in array {}
for i:j in array do end
for let i,j in array {}
for let i, let j in array {}

for leek in getLeeks() do
	print(leek.getLife())
	for i in leek.cells {
		print(i + 1)
	}
	leek.cells.each(x -> print(x))
end
