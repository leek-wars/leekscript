var digits = [1]

for var p = 1; p <= 100; p++ {
	for var j = 0; j < digits.size(); j++ {
		digits[j] *= p
	}
	for var i = 0; i < digits.size(); i++ {
		if (9 < digits[i]) {
			let m = digits[i] % 10
			var q = (digits[i] - m) / 10
			digits[i] = m
			if (i < digits.size() - 1) {
				digits[i + 1] += q
			} else {
				digits += q
				break
			}
		}
	}
	var n = digits.size() - 1
	while (9 < digits[n]) {
		let mo = digits[n] % 10
		let qu = (digits[n] - mo) / 10
		digits[n] = mo
		digits += qu
		n++
	}
}
// print(~digits)
digits.sum()
