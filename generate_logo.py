import math

width = 1024
height = 1024
cx = width / 2.0
cy = height / 2.0

# 6 tips: angles -90 (top), -30, 30, 90 (bottom), 150, 210
# 6 valleys: angles -60, 0, 60, 120, 180, -120

def polar_pt(angle_deg, r):
    rad = math.radians(angle_deg)
    return (cx + r * math.cos(rad), cy + r * math.sin(rad))

# Radii
R_gold_border = 485.0
r_gold_border = 310.0

R_black_border = 475.0
r_black_border = 300.0

R_star = 428.0
r_star = 270.0

def make_star_path(R, r):
    tip_angles = [-90, -30, 30, 90, 150, 210]
    valley_angles = [-60, 0, 60, 120, 180, -120]
    pts = []
    for i in range(6):
        pts.append(polar_pt(tip_angles[i], R))
        pts.append(polar_pt(valley_angles[i], r))
    d = "M " + " L ".join(f"{p[0]:.2f},{p[1]:.2f}" for p in pts) + " Z"
    return d

star_path_gold = make_star_path(R_gold_border, r_gold_border)
star_path_black = make_star_path(R_black_border, r_black_border)

# 12 facets
tips = [polar_pt(a, R_star) for a in [-90, -30, 30, 90, 150, 210]]
valleys = [polar_pt(a, r_star) for a in [-60, 0, 60, 120, 180, -120]]

# The 12 triangles:
triangles = [
    (tips[0], valleys[0]),   # 0: top right
    (valleys[0], tips[1]),   # 1: top-right upper
    (tips[1], valleys[1]),   # 2: top-right lower
    (valleys[1], tips[2]),   # 3: bottom-right upper
    (tips[2], valleys[2]),   # 4: bottom-right lower
    (valleys[2], tips[3]),   # 5: bottom right
    (tips[3], valleys[3]),   # 6: bottom left
    (valleys[3], tips[4]),   # 7: bottom-left lower
    (tips[4], valleys[4]),   # 8: bottom-left upper
    (valleys[4], tips[5]),   # 9: top-left lower
    (tips[5], valleys[5]),   # 10: top-left upper
    (valleys[5], tips[0]),   # 11: top left (pale cream)
]

facet_colors = [
    '#F7C93A', # 0: bright yellow
    '#FED852', # 1: top-right upper
    '#F5B92D', # 2: top-right lower
    '#F0A620', # 3: bottom-right upper
    '#D88915', # 4: bottom-right lower
    '#9B620E', # 5: bottom right
    '#B87C17', # 6: bottom left
    '#D88E18', # 7: bottom-left lower
    '#F3BE2D', # 8: bottom-left upper
    '#FED54B', # 9: top-left lower
    '#FCE278', # 10: top-left upper
    '#FFF8D6'  # 11: top left (pale cream)
]

# Skull grid
# 10 columns x 11 rows
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
grid_cols = 10
grid_rows = len(skull_grid)
skull_start_x = cx - (grid_cols * block_w) / 2.0
skull_start_y = cy - (grid_rows * block_h) / 2.0 + 8.0

svg_parts = []
svg_parts.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="{width}" height="{height}">')
svg_parts.append(f'<rect width="{width}" height="{height}" fill="#000000" />')

# Outer gold border line
svg_parts.append(f'<path d="{star_path_gold}" fill="#000000" stroke="#DFAC16" stroke-width="7" stroke-linejoin="miter" stroke-miterlimit="5" />')

# Thick black border
svg_parts.append(f'<path d="{star_path_black}" fill="#000000" stroke="#000000" stroke-width="2" stroke-linejoin="miter" />')

# Inner star facet triangles
for i, (p1, p2) in enumerate(triangles):
    c = facet_colors[i]
    d = f"M {cx:.2f},{cy:.2f} L {p1[0]:.2f},{p1[1]:.2f} L {p2[0]:.2f},{p2[1]:.2f} Z"
    svg_parts.append(f'<path d="{d}" fill="{c}" stroke="{c}" stroke-width="1.2" stroke-linejoin="round" />')

# Subtle facet ridge line from top to bottom
svg_parts.append(f'<line x1="{cx}" y1="{tips[0][1]}" x2="{cx}" y2="{tips[3][1]}" stroke="rgba(0,0,0,0.15)" stroke-width="1" />')

# Skull blocks
for r, row in enumerate(skull_grid):
    for c, ch in enumerate(row):
        if ch == '#':
            bx = skull_start_x + c * block_w
            by = skull_start_y + r * block_h
            svg_parts.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{block_w + 0.5:.1f}" height="{block_h + 0.5:.1f}" fill="#000000" />')

svg_parts.append('</svg>')

with open('krunker_logo.svg', 'w') as f:
    f.write("\n".join(svg_parts))

print("SVG generated successfully")
