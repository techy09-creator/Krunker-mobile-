import math

# Supersampled resolution
W = 1024
H = 1024
cx = W / 2.0
cy = H / 2.0

# 6 tips: angles -90 (top), -30, 30, 90 (bottom), 150, 210
# 6 valleys: angles -60, 0, 60, 120, 180, -120

def polar_pt(angle_deg, r):
    rad = math.radians(angle_deg)
    return (cx + r * math.cos(rad), cy + r * math.sin(rad))

R_outer_gold = 485.0
r_outer_gold = 310.0

R_outer_black = 472.0
r_outer_black = 298.0

R_star = 430.0
r_star = 270.0

tip_angles = [-90, -30, 30, 90, 150, 210]
valley_angles = [-60, 0, 60, 120, 180, -120]

def make_star_polygon(R, r):
    poly = []
    for i in range(6):
        poly.append(polar_pt(tip_angles[i], R))
        poly.append(polar_pt(valley_angles[i], r))
    return poly

poly_gold = make_star_polygon(R_outer_gold, r_outer_gold)
poly_black = make_star_polygon(R_outer_black, r_outer_black)

tips = [polar_pt(a, R_star) for a in tip_angles]
valleys = [polar_pt(a, r_star) for a in valley_angles]

triangles = [
    (tips[0], valleys[0]),   # 0
    (valleys[0], tips[1]),   # 1
    (tips[1], valleys[1]),   # 2
    (valleys[1], tips[2]),   # 3
    (tips[2], valleys[2]),   # 4
    (valleys[2], tips[3]),   # 5
    (tips[3], valleys[3]),   # 6
    (valleys[3], tips[4]),   # 7
    (tips[4], valleys[4]),   # 8
    (valleys[4], tips[5]),   # 9
    (tips[5], valleys[5]),   # 10
    (valleys[5], tips[0]),   # 11
]

def hex2rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

facet_colors = [
    hex2rgb('#F7C93A'), # 0: bright yellow
    hex2rgb('#FED852'), # 1: top-right upper
    hex2rgb('#F5B92D'), # 2: top-right lower
    hex2rgb('#F0A620'), # 3: bottom-right upper
    hex2rgb('#D88915'), # 4: bottom-right lower
    hex2rgb('#9B620E'), # 5: bottom right
    hex2rgb('#B87C17'), # 6: bottom left
    hex2rgb('#D88E18'), # 7: bottom-left lower
    hex2rgb('#F3BE2D'), # 8: bottom-left upper
    hex2rgb('#FED54B'), # 9: top-left lower
    hex2rgb('#FCE278'), # 10: top-left upper
    hex2rgb('#FFF8D6')  # 11: top left
]

COLOR_BLACK = (0, 0, 0)
COLOR_GOLD_BORDER = hex2rgb('#DFAC16')

# Skull grid: 10 columns x 11 rows
skull_grid = [
    "  ######  ",
    "  ######  ",
    " ######## ",
    "##########",
    "##  ##  ##",
    "##  ##  ##",
    "##########",
    " ######## ",
    " ######## ",
    " ## ## ## ",
    " ##    ## ",
]

block_w = 31.0
block_h = 31.0
skull_cols = 10
skull_rows = len(skull_grid)
skull_x0 = cx - (skull_cols * block_w) / 2.0
skull_y0 = cy - (skull_rows * block_h) / 2.0 + 8.0

# Pre-filter: determine bounding boxes for triangles
def point_in_triangle(px, py, p0, p1, p2):
    dX = px - p2[0]
    dY = py - p2[1]
    dX21 = p2[0] - p1[0]
    dY12 = p1[1] - p2[1]
    D = dY12 * (p0[0] - p2[0]) + dX21 * (p0[1] - p2[1])
    s = dY12 * dX + dX21 * dY
    t = (p2[1] - p0[1]) * dX + (p0[0] - p2[0]) * dY
    if D < 0:
        return s <= 0 and t <= 0 and (s + t) >= D
    return s >= 0 and t >= 0 and (s + t) <= D

def point_in_polygon(px, py, poly):
    inside = False
    n = len(poly)
    p1x, p1y = poly[0]
    for i in range(1, n + 1):
        p2x, p2y = poly[i % n]
        if py > min(p1y, p2y):
            if py <= max(p1y, p2y):
                if px <= max(p1x, p2x):
                    if p1y != p2y:
                        xinters = (py - p1y) * (p2x - p1x) / (p2y - p1y) + p1x
                    if p1x == p2x or px <= xinters:
                        inside = not inside
        p1x, p1y = p2x, p2y
    return inside

# Precompute triangle bboxes
tri_bboxes = []
c_pt = (cx, cy)
for p1, p2 in triangles:
    min_x = min(cx, p1[0], p2[0])
    max_x = max(cx, p1[0], p2[0])
    min_y = min(cy, p1[1], p2[1])
    max_y = max(cy, p1[1], p2[1])
    tri_bboxes.append((min_x, max_x, min_y, max_y))

# Render buffer
buf = bytearray(W * H * 3)

for y in range(H):
    row_offset = y * W * 3
    for x in range(W):
        # Default background: black
        r, g, b = COLOR_BLACK

        # Check if in star polygon
        if point_in_polygon(x, y, poly_gold):
            if not point_in_polygon(x, y, poly_black):
                r, g, b = COLOR_GOLD_BORDER
            else:
                # Inside inner black border / facets
                in_facet = False
                for idx, (p1, p2) in enumerate(triangles):
                    bx0, bx1, by0, by1 = tri_bboxes[idx]
                    if bx0 <= x <= bx1 and by0 <= y <= by1:
                        if point_in_triangle(x, y, c_pt, p1, p2):
                            r, g, b = facet_colors[idx]
                            in_facet = True
                            break
                if not in_facet:
                    r, g, b = COLOR_BLACK

        # Check if inside skull block
        if skull_x0 <= x < skull_x0 + skull_cols * block_w and skull_y0 <= y < skull_y0 + skull_rows * block_h:
            col_idx = int((x - skull_x0) / block_w)
            row_idx = int((y - skull_y0) / block_h)
            if 0 <= col_idx < skull_cols and 0 <= row_idx < skull_rows:
                if skull_grid[row_idx][col_idx] == '#':
                    r, g, b = COLOR_BLACK

        pixel_offset = row_offset + x * 3
        buf[pixel_offset] = r
        buf[pixel_offset + 1] = g
        buf[pixel_offset + 2] = b

header = f"P6\n{W} {H}\n255\n".encode('ascii')
with open('krunker_logo_rendered.ppm', 'wb') as f:
    f.write(header)
    f.write(buf)

print("Rendering complete!")
