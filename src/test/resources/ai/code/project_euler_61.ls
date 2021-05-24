var tri = []
for (var t = 0; t < 142; ++t) {
	print(t * (t + 1) / 2)
	//Array.insert(tri, true, t * (t + 1) / 2) 
}

print(tri)

/*
var squ = []
for (var s = 0; s < 102; ++s) { Array.insert(squ, true, s * s) }
var pen = []
for (var p = 0; p < 83; ++p) { Array.insert(pen, true, p * (3 * p - 1) / 2) }
var hex = []
for (var h = 0; h < 72; ++h) { Array.insert(hex, true, h * (2 * h - 1)) }
var hep = []
for (var g = 0; g < 65; ++g) { Array.insert(hep, true, g * (5 * g - 3) / 2) }
var oct = []
for (var o = 0; o < 61; ++o) { Array.insert(oct, true, o * (3 * o - 2)) }

var P = [tri, squ, pen, hex, hep, oct]
var poly = [0, 0, 0, 0, 0, 0]

print(P)

/*

var NUL = []
for (var nul = 1000; nul <= 9999; ++nul) {
	if (tri[nul] == null && squ[nul] == null && pen[nul] == null && 
		hex[nul] == null && hep[nul] == null && oct[nul] == null) {
		Array.insert(NUL, true, nul)
	}
}
var min = 1000
var max = 9999

for (var a = min; a <= max; ++a) {
	if NUL[a] continue

	for (var pa = 0; pa < 6; ++pa) {
	
		if (P[pa][a] == null) continue
		poly[pa] = 1
		var al = a % 100
		var af = (a - al) / 100
		
		for (var b = Number.max(min, al * 100); b < Number.min(max, (al + 1) * 100); ++b) {
			if NUL[b] continue
		
			for (var pb = 0; pb < 6; ++pb) {
			
				if (poly[pb] or P[pb][b] == null) continue
				poly[pb] = 1
				var bl = b % 100
		
				for (var c = Number.max(min, bl * 100); c < Number.min(max, (bl + 1) * 100); ++c) {
					if NUL[c] continue
				
					for (var pc = 0; pc < 6; ++pc) {
							
						if (poly[pc] or P[pc][c] == null) continue
						poly[pc] = 1
						var cl = c % 100
					
						for (var d = Number.max(min, cl * 100); d < Number.min(max, (cl + 1) * 100); ++d) {
							if NUL[d] continue
						
							for (var pd = 0; pd < 6; ++pd) {
								
								if (poly[pd] or P[pd][d] == null) continue
								poly[pd] = 1
								var dl = d % 100
						
								for (var e = Number.max(min, dl * 100); e < Number.min(max, (dl + 1) * 100); ++e) {
									if NUL[e] continue
								
									for (var pe = 0; pe < 6; ++pe) {
										
										if (poly[pe] or P[pe][e] == null) continue
										poly[pe] = 1
										var el = e % 100
								
										for (var f = Number.max(min, el * 100); f < Number.min(max, (el + 1) * 100); ++f) {
											if NUL[f] continue
											
											if (f % 100 == af) {
												for (var pf = 0; pf < 6; ++pf) {
													if (!poly[pf] and P[pf][f]) {
														print(a + " " + b + " " + c + " " + d + " " + e + " " + f)
														return "sum : " + (a + b + c + d + e + f)
													}
												}
											}
										}
										poly[pe] = 0
									}
								}
								poly[pd] = 0
							}
						}
						poly[pc] = 0
					}
				}
				poly[pb] = 0
			}
		}
		poly[pa] = 0
	}
}