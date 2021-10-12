def readt(s) :
    rows = s.strip().split("\n")
    Y = len(rows)
    X = len(rows[0])
    assert all(len(row) == X for row in rows)
    return X, Y, [x not in (' ', '.') for row in rows for x in row]

def savet(X, Y, F) :
    return "".join(''.join('x' if F[y * X + x] else '.' for x in range(X)) + '\n' for y in range(Y))
