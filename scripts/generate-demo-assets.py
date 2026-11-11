"""Generate original, offline SVG illustrations. No third-party images or fonts."""
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "frontend" / "public" / "demo"
OUT.mkdir(parents=True, exist_ok=True)

def save(name, background, title, subtitle, shapes):
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="600" height="800" viewBox="0 0 600 800" role="img" aria-labelledby="title">
<title id="title">{title}</title><rect width="600" height="800" fill="{background}"/>
{shapes}
<text x="38" y="66" fill="#ffffff" font-family="Georgia,serif" font-size="24" letter-spacing="2">{title}</text>
<text x="40" y="98" fill="#ffffff" font-family="sans-serif" font-size="11" letter-spacing="3">{subtitle}</text>
<path d="M40 124H560" stroke="#ffffff" stroke-opacity=".35"/>
</svg>'''
    (OUT / f"{name}.svg").write_text(svg, encoding="utf-8")

save("coastal", "#679ba4", "A POSTCARD FROM THE SEA", "COLLECT LITTLE MOMENTS", '''
<circle cx="444" cy="241" r="65" fill="#f4d99a"/><path d="M0 390Q140 310 300 400T600 380V800H0" fill="#346e7e"/>
<path d="M0 490Q120 410 280 490T600 440V800H0" fill="#224f60"/><path d="M0 635Q190 540 380 665T600 650V800H0" fill="#e5cfaa"/>
<path d="M0 620Q190 525 380 650T600 635" fill="none" stroke="#fff3d8" stroke-width="12"/>
<path d="M130 280l18-10 18 10m30 20 15-8 15 8" fill="none" stroke="#fff4d7" stroke-width="3"/>
<rect x="432" y="640" width="114" height="90" rx="2" fill="#f7eee0" transform="rotate(9 489 685)"/>
<text x="450" y="691" fill="#517681" font-size="22" font-family="Georgia,serif" transform="rotate(9 489 685)">Hello.</text>''')
save("mountain", "#879988", "INTO THE QUIET", "A FIELD JOURNAL", '''
<circle cx="429" cy="243" r="58" fill="#e8d9b3"/><path d="M-60 590 185 260 370 580 470 370 660 650V800H0" fill="#4e685c"/>
<path d="m185 260-56 76 55-15 51 22z" fill="#dedecb"/><path d="M0 690 290 420 600 730V800H0" fill="#304c43"/>
<path d="M70 800 180 624 255 800M350 800 445 580 540 800" fill="#203f38"/>
<path d="M275 800q-80-100 30-130t-20-110" fill="none" stroke="#c2bc92" stroke-width="10"/>''')
save("pet", "#bd8e71", "THE MAIN CHARACTER", "SMALL PAWS, BIG PERSONALITY", '''
<ellipse cx="300" cy="701" rx="205" ry="40" fill="#9f6b52"/>
<ellipse cx="300" cy="576" rx="145" ry="151" fill="#e9d3ae"/>
<path d="M155 380 135 214 264 302M445 380 465 214 336 302" fill="#e9d3ae"/>
<path d="m167 300-9-42 53 43m222-1 9-42-53 43" fill="#d89e92"/>
<ellipse cx="300" cy="399" rx="164" ry="141" fill="#efddbd"/>
<ellipse cx="240" cy="399" rx="12" ry="18" fill="#393126"/><ellipse cx="360" cy="399" rx="12" ry="18" fill="#393126"/>
<path d="m285 443 15 15 15-15" fill="#a36b60"/><path d="M300 458v15m0 0q-24 20-36-2m36 2q24 20 36-2" fill="none" stroke="#674b3b" stroke-width="4"/>
<path d="m190 500 110 64 110-64-20 80H210" fill="#537a72"/><circle cx="300" cy="574" r="14" fill="#d6b875"/>''')
save("pet-duo", "#a2aaa0", "BETTER TOGETHER", "TWO LITTLE FRIENDS", '''
<rect x="44" y="192" width="512" height="530" rx="230" fill="#d2c8b6"/>
<ellipse cx="210" cy="600" rx="105" ry="120" fill="#c18254"/><ellipse cx="400" cy="600" rx="100" ry="120" fill="#f3e4c7"/>
<ellipse cx="210" cy="433" rx="107" ry="115" fill="#d79b65"/><ellipse cx="112" cy="443" rx="33" ry="89" fill="#a66f48"/>
<ellipse cx="306" cy="443" rx="29" ry="89" fill="#a66f48"/>
<path d="m310 420 13-141 86 91 76-90 21 140" fill="#f3e4c7"/><ellipse cx="407" cy="440" rx="105" ry="104" fill="#f3e4c7"/>
<g fill="#36342f"><circle cx="175" cy="425" r="9"/><circle cx="245" cy="425" r="9"/><ellipse cx="210" cy="463" rx="17" ry="12"/><circle cx="372" cy="431" r="9"/><circle cx="442" cy="431" r="9"/></g>
<path d="m395 466 12 12 12-12" fill="#c38b7a"/><path d="M173 543h74M371 543h73" stroke="#719194" stroke-width="18"/>''')
save("city", "#535d79", "WALK A LITTLE SLOWER", "THE CITY IN BETWEEN", '''
<circle cx="448" cy="250" r="62" fill="#e8bca0"/><path d="M0 450 100 380V800H0" fill="#303f58"/>
<path d="M92 280H265V800H92" fill="#a57567"/><path d="M265 380H440V800H265" fill="#717a8d"/>
<path d="M440 330H600V800H440" fill="#3c4d65"/>
<g fill="#e1c19b"><path d="M117 327h35v60h-35zm67 0h35v60h-35zM117 417h35v60h-35zm67 0h35v60h-35zM117 507h35v60h-35zm67 0h35v60h-35zM298 427h40v69h-40zm68 0h40v69h-40zM298 527h40v69h-40zm68 0h40v69h-40zM473 377h35v60h-35zm65 0h35v60h-35zM473 477h35v60h-35zm65 0h35v60h-35z"/></g>
<path d="M0 740 600 667V800H0" fill="#252d40"/><path d="M0 765 600 692" stroke="#dabda0" stroke-width="4"/>
<path d="M380 712V590q0-35 31-35" fill="none" stroke="#222c3d" stroke-width="8"/><circle cx="413" cy="555" r="14" fill="#edcc99"/>''')
save("color", "#d8cbb8", "A DAY IN THREE COLORS", "YOUR EVERYDAY EDIT", '''
<rect x="40" y="163" width="520" height="175" fill="#bc705b"/><circle cx="410" cy="250" r="66" fill="#e4b19a"/>
<path d="M40 338 180 180 310 338" fill="#934f45"/><rect x="40" y="355" width="520" height="175" fill="#647f76"/>
<path d="M40 485q140-150 270 0t250-10v55H40" fill="#b1bfa3"/><path d="M40 520q160-130 320-10t200-50v70H40" fill="#3e5c53"/>
<rect x="40" y="547" width="520" height="175" fill="#d2ad65"/><path d="M230 722V630a70 70 0 0 1 140 0v92" fill="#f0ddae"/>
<path d="M0 800 600 742" stroke="#b6a691" stroke-width="2"/>''')
print(f"Generated 6 SVG illustrations in {OUT}")
