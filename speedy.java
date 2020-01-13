import java.util.Scanner;

class speedy {
	////////////////////////////////////////////////////////////////	
	///
	/// CONSTANTS
	///

	static final int SCREENW = 80;
	static final int SCREENH = 25;
	static final int FBSZ    = SCREENH * SCREENW;

	static final int QUOTEX = 0;
	static final int QUOTEY = 0;

	static final int MENUX = 0;
	static final int MENUY = QUOTEY + 3;
	static final int MENUW = 24;
	static final int MENUH = 6;

	static final int TOTALSX = MENUX;
	static final int TOTALSY = MENUY + MENUH + 3;
	static final int TOTALSW = MENUW;
	static final int TOTALSH = 10;

	static final int LISTX = MENUX + MENUW + 3;
	static final int LISTY = MENUY;
	static final int LISTW = SCREENW - LISTX - 4;
	static final int LISTH = SCREENH - LISTY - 3 - 5;

	static final int MSGX = LISTX;
	static final int MSGY = LISTY + LISTH + 2;
	static final int MSGW = LISTW;
	static final int MSGH = 3;

	static final int MAXINPUTBUF = 64;

	static final int MAXOFFENDER = 6;
	static final int MAXNAME     = 64;

	static final int SPEEDCLASSES = 7;

	static final int[] speedThreshold = { 60,  55,  50,  45, 40,  35, 30 };
	static final int[] speedFine      = {  0, 150, 100, 100, 50, 100, 50 };
	static final int[] speedPoints    = {  0,   6,   6,   3,  3,   0,  0 };

	static final String[] quote = {
		"i *am* the law",                                              "cops",
		"go on, judge! shit on 'em!",                                  "jury",
		"we're going downtown gonna beat up the drunks",               "jello",
		"who the hell needs a bill of rights? i'm bill and i'm right", "ol' bill",
		"<inspirational quote>",                                       "the developer" };

	////////////////////////////////////////////////////////////////
	///
	/// GLOBALS
	///

	static char[][] frameBuffer;
	static int boundBuffer;

	static String[] name;         /* name of the offender */
	static int[]    licence;      /* time licence held in years */
	static int[]    fine;         /* total raw fine in local currency */
	static int[]    points;       /* total penalty points */
	static int[][]  classOffence; /* number of offences in each speed class */

	static int nOffender;

	static int quoteIdx;

	static int demoMode;
	static int demoIdx;

	static Scanner input;
	static String lastMsg;

	////////////////////////////////////////////////////////////////
	///
	/// RENDER ROUTINES
	///

	static void
	blit () {
		System.out.print (frameBuffer[boundBuffer]);
		return; }

	static void
	clearscreen () {
		/* good terminals can be cleared with the \x1b[2J escape
		 * sequence. eclipse doesn't have a good terminal so you
		 * clear it by spaming it with line feeds */
		char[] buf = new char[128];
		for (int i = 0; i < 128; ++i) {
			buf[i] = 0x0a; }

		System.out.print (buf);

		return; }

	static void
	clearbuffer () {
		/* the framebuffer is cleared by filling it with
		 * spaces, and then inserting a line feed at the
		 * end of each row */
		for (int i = 0; i < FBSZ; ++i) {
			frameBuffer[boundBuffer][i] = 0x20; }

		int idx = SCREENW - 1;
		for (int i = 0; i < SCREENH; ++i) {
			frameBuffer[boundBuffer][idx] = 0x0a;
			idx += SCREENW; }

		/* null terminate because i don't know how print works.
		 * i assume that because it knows how big the array is,
		 * it write()s the whole thing in one but just in case. */
		frameBuffer[boundBuffer][FBSZ] = 0;

		return; }

	static void
	clear () {
		clearbuffer ();
		clearscreen ();
		return; }

	/* XXX: for simplicity none of the drawing functions do any clipping.
	 * so drawing out of bounds will cause artifacting, buffer overflows,
	 * segfaults... for this program it doesn't matter; let it die */

	static void
	drawline (int x, int y, int vert, int len) {
		char c    = '|';
		int  step = SCREENW;
		if (vert == 0) {
			c    = '-';
			step = 1; }

		int idx = y * SCREENW + x;
		while (len-- > 0) {
			frameBuffer[boundBuffer][idx] = c;
			idx += step; }

		return; }

	static void
	drawstring2 (int x, int y, int w, String s) {
		/* draw string s clipped to width w */
		int clip = 0;
		int len  = s.length ();
		if (len > w) {
			clip = 1;
			len  = w - 1; }

		int idx = y * SCREENW + x;
		for (int i = 0; i < len; ++i) {
			frameBuffer[boundBuffer][idx++] = s.charAt (i); }

		if (clip != 0) {
			/* indicate truncation */
			frameBuffer[boundBuffer][idx] = '#'; }

		return; }

	static void
	drawstring (int x, int y, String s) {
		drawstring2 (x, y, s.length (), s);
		return; }

	static void
	drawbox2 (int x, int y, int w, int h, String title) {
		drawstring (x,         y,         ".");
		drawline   (x + 1,     y,         0, w);
		drawstring (x + w + 1, y,         ".");
		drawline   (x,         y + 1,     1, h);
		drawline   (x + w + 1, y + 1,     1, h);
		drawstring (x,         y + h + 1, "`");
		drawline   (x + 1,     y + h + 1, 0, w);
		drawstring (x + w + 1, y + h + 1, "'");

		int tlen = title.length ();
		if (tlen > 0) {
			if (tlen >= w) {
				tlen = w; }

			drawstring (x + 3,        y, "[ ");
			drawstring (x + 5,        y, title);
			drawstring (x + 5 + tlen, y, " ]"); }

		return; }

	static void
	drawbox (int x, int y, int w, int h) {
		drawbox2 (x, y, w, h, "");
		return; }

	static void
	drawmenu (int x, int y, int w, int h, String[] opt, String title) {
		drawbox2 (x, y, w, h, title);

		/* TODO: fit columns to width */

		/* we leave one cell of padding between the text and border */
		x += 2;
		y += 2;
		w -= 2;
		h -= 2;

		int longest = 0;

		int yp = y;
		int n  = opt.length;
		for (int i = 0; i < n; ++i) {
			drawstring (x, yp++, opt[i]);

			/* find the longest string in this column so we
			 * can draw the next column without overlap */
			int slen = opt[i].length();
			if (slen >= longest) {
				longest = slen + 1; }

			if (i % h == h-1) {
				yp = y;
				x += longest;
				longest = 0; }}

		return; }

	////////////////////////////////////////////////////////////////
	///
	/// PROGRAM PROPER
	///

	static void
	drawtotals (int x, int y, int w, int h) {
		drawbox2 (x, y, w, h, "totals");
		x += 2;
		y += 2;

		drawstring (x, y, "speed class");
		for (int i = 0; i < SPEEDCLASSES; ++i) {
			String speed = String.valueOf (speedThreshold[i]);
			String total = String.valueOf (totalclassoffences (i));

			drawstring (x + 2, y + i + 1, ">");
			drawstring (x + 3, y + i + 1, speed);
			drawstring (x + 5, y + i + 1, ": ");
			drawstring (x + 7, y + i + 1, total); }

		x += w >> 1;
		drawline (x, y, 1, h-2);
		x += 2;

		int totalFines  = 0;
		int totalPoints = 0;
		for (int i = 0; i < nOffender; ++i) {
			totalFines  += gettotalfine (i);
			totalPoints += points[i]; }

		drawstring (x, y++, "fines");
		drawstring (x, y,   "  " + String.valueOf (totalFines));
		y += 2;

		drawline (x, y, 0, (w >> 1) - 4);
		y += 2;

		drawstring (x, y++, "points");
		drawstring (x, y,   "  " + String.valueOf (totalPoints));

		return; }

	static void
	drawoffender (int x, int y, int w, int offender) {
		String[] field = {
			"name",
			"licence",
			"fine",
			"points",
			"flags" };

		for (int i = 0; i < field.length; ++i) {
			drawstring (x, y + i, field[i] + ":"); }

		/* length of the longest field plus 2 for ": " */
		x += 9;

		if (name[offender].length () > 0) {
			drawstring2 (x, y, w - 8, name[offender]); }

		y++;

		drawstring (x, y++, String.valueOf (licence[offender]));
		drawstring (x, y++, String.valueOf (gettotalfine (offender)));
		drawstring (x, y++, String.valueOf (points [offender]));

		char[] flag = { '-', '-', '-' };
		if (courtappearance (offender) < 0) { flag[0] = 'C'; }
		if (revokelicence   (offender) < 0) { flag[1] = 'R'; }

		drawstring (x, y++, String.valueOf (flag));

		return; }

	static void
	drawoffenders (int x, int y, int w, int h) {
		drawbox2 (x, y, w, h, "offenders");
		x += 2;
		y += 2;

		int blockW = 15;
		int blockH = 7;
		int blocksPerRow = w / blockW;

		int xp = x;
		for (int i = 0; i < nOffender; ++i) {
			String header = " " + i + " ";

			drawline     (xp, y, 0, blockW);
			drawstring   (xp + (blockW >> 1) - 1, y, header);
			drawoffender (xp, y + 1, blockW - 1, i);

			xp += blockW + 1;
			if (i % blocksPerRow == blocksPerRow - 1) {
				xp = x;
				y += blockH; }}

		return; }

	static int
	getspeedclass (int speed) {
		for (int i = 0; i < SPEEDCLASSES; ++i) {
			if (speed > speedThreshold[i]) {
				return i; }}

		return -1; }

	static int
	totalclassoffences (int clas) {
		int total = 0;
		for (int i = 0; i < nOffender; ++i) {
			total += classOffence[i][clas]; }

		return total; }

	static int
	gettotalfine (int offender) {
		int total = fine[offender];
		if (licence[offender] < 2) {
			total *= 2; }

		return total; }

	static int
	courtappearance (int offender) {
		if (classOffence[offender][0] > 0) {
			return -1; }

		return 0; }

	static int
	revokelicence (int offender) {
		if (points[offender] >= 6) {
			return -1; }

		return 0; }

	static void
	clearoffences (int offender) {
		fine  [offender] = 0;
		points[offender] = 0;

		for (int i = 0; i < SPEEDCLASSES; ++i) {
			classOffence[offender][i] = 0; }

		return; }

	static void
	initoffender (int offender) {
		licence[offender] = 0;
		clearoffences (offender);

		for (int i = 0; i < MAXNAME; ++i) {
			name[offender] = ""; }

		return; }

	static int __addState = 0;

	static int
	add () {
		final int as_init    = 0;
		final int as_menu    = 1;
		final int as_name    = 2;
		final int as_licence = 3;
		final int as_speed   = 4;
		final int as_quit    = 5;

		if (nOffender >= MAXOFFENDER) {
			return -1; }

		int offender = nOffender;
		int speedClass;

		String[] option = {
			"n) set name",
			"l) set licence",
			"o) add offence",
			"q) quit" };

		drawmenu     (MENUX,     MENUY,     MENUW,   MENUH, option, "menu");
		drawtotals   (TOTALSX,   TOTALSY,   TOTALSW, TOTALSH);
		drawbox2     (LISTX,     LISTY,     LISTW,   LISTH, "new offender");
		drawoffender (LISTX + 2, LISTY + 2, LISTW,   offender);

		blit ();

		String tmp;
		switch (__addState) {
			case as_init:
				initoffender (offender);
				__addState = as_menu;
				break;

			case as_menu:
				lastMsg = "";
				write (1, "option >> ");
				switch (getinput().charAt(0)) {
					case 'n': __addState = as_name;    break;
					case 'l': __addState = as_licence; break;
					case 'o': __addState = as_speed;   break;
					case 'q': __addState = as_quit;    break;
					default : lastMsg = "invalid option"; break; }

				break;

			case as_name:
				write (1, "name >> ");
				tmp = getinput ();
				if (tmp.charAt(0) != '\n') {
					name[offender] = tmp; }

				__addState = as_menu;
				break;

			case as_licence:
				write (1, "licence (years) >> ");
				tmp = getinput ();
				if (tmp.charAt(0) != '\n') {
					int v = atoi (tmp);
					if (v < 0 || v > 1024) {
						lastMsg = "invalid value";
						return 0; }

					lastMsg = "";
					licence[offender] = v; }

				__addState = as_menu;
				break;

			case as_speed:
				write (1, "speed (mph) >> ");
				tmp = getinput ();
				if (tmp.charAt(0) == '\n') {
					fine  [offender] = 0;
					points[offender] = 0;
					clearoffences (offender);
					break; }

				else {
					int v = atoi (tmp);
					if (v < 0 || v > 1024) {
						lastMsg = "invalid value";
						return 0; }

					lastMsg = "";
					speedClass = getspeedclass (v);
					if (speedClass >= 0) {
						fine  [offender] += speedFine  [speedClass];
						points[offender] += speedPoints[speedClass];

						classOffence[offender][speedClass]++; }}

				__addState = as_menu;
				break;

			case as_quit:
				if (name[offender].length() > 0) {
					nOffender++; }

				__addState = as_init;
				return -1; }

		return 0; }

	static int
	delete () {
		if (nOffender <= 0) {
			return -1; }

		drawbox (MENUX, MENUY, MENUW, MENUH);
		blit ();

		write (1, "delete which? (0 - " + (nOffender-1) + ") >> ");
		String tmp = getinput ();
		if (tmp.charAt(0) == '\n') {
			return -1; }

		int idx = atoi (tmp);
		if (idx > nOffender || idx < 0) {
			lastMsg = "invalid value";
			return 0; }

		for (int i = idx + 1; i < nOffender; ++i) {
			name   [i - 1] = name   [i];
			fine   [i - 1] = fine   [i];
			points [i - 1] = points [i];
			licence[i - 1] = licence[i];

			for (int j = 0; j < SPEEDCLASSES; ++j) {
				classOffence[i - 1][j] = classOffence[i][j]; }}

		nOffender--;
		return -1; }

	static int
	drawmainmenu () {
		String   blank  = "-) ---";
		String[] option = {
			"n) new",
			"d) delete",
			"q) quit" };

		/* dont draw the new option if we're out of memory */
		if (nOffender >= MAXOFFENDER) {
			option[0] = blank; }

		/* or delete if there's nothing to delete */
		if (nOffender <= 0) {
			option[1] = blank; }

		drawmenu (MENUX, MENUY, MENUW, MENUH, option, "menu");

		return 0; }

	static void
	inspire (int x, int y) {
		/* quote is even index - quoted is odd */
		int qlen = quote[quoteIdx    ].length();
		int nlen = quote[quoteIdx + 1].length();

		int qcx = (SCREENW - qlen - 2) >> 1;
		int ncx = (SCREENW - nlen    ) >> 1;

		drawstring (x + qcx, y++, "\"" + quote[quoteIdx    ] + "\"");
		drawstring (x + ncx, y++,        quote[quoteIdx + 1]       );

		return; }

	static void
	bootanim () {
		int menuw   = 0;
		int menuh   = 0;
		int totalsw = 0;
		int totalsh = 0;
		int listw   = 0;
		int listh   = 0;
		int msgw   = 0;
		int msgh   = 0;

		/* first lerp in y */
		float t = 0.f;
		while (t < 1.1f) {
			clear ();

			menuh   = lerp (0, MENUH,   t);
			totalsh = lerp (0, TOTALSH, t);
			listh   = lerp (0, LISTH,   t);
			msgh    = lerp (0, MSGH,    t);

			t += .1f;
			drawbox (MENUX,   MENUY,   menuw,   menuh);
			drawbox (TOTALSX, TOTALSY, totalsw, totalsh);
			drawbox (LISTX,   LISTY,   listw,   listh);
			drawbox (MSGX,    MSGY,    msgw,    msgh);

			blit ();
			sleep (33); }

		/* and then in x */
		t = 0.f;
		while (t < 1.1f) {
			clear ();

			menuw   = lerp (0, MENUW,   t);
			totalsw = lerp (0, TOTALSW, t);
			listw   = lerp (0, LISTW,   t);
			msgw    = lerp (0, MSGW,    t);

			t += .1f;
			drawbox (MENUX,   MENUY,   menuw,   menuh);
			drawbox (TOTALSX, TOTALSY, totalsw, totalsh);
			drawbox (LISTX,   LISTY,   listw,   listh);
			drawbox (MSGX,    MSGY,    msgw,    msgh);

			blit ();
			sleep (33); }

		/* now draw the entire main screen into a second buffer */
		boundBuffer = 1;
		clear ();

		inspire (QUOTEX, QUOTEY);
		drawmainmenu ();
		drawoffenders (LISTX, LISTY, LISTW, LISTH);
		drawtotals (TOTALSX, TOTALSY, TOTALSW, TOTALSH);
		drawmsg (MSGX, MSGY, MSGW, MSGH);

		/* and copy it a column at a time into the front buffer */
		boundBuffer = 0;

		int c = 0;
		do {
			for (int i = 0; i < 2; ++i) {
				for (int r = 0; r < SCREENH; ++r) {
					int idx = r * SCREENW + c;
					frameBuffer[0][idx] = frameBuffer[1][idx]; }

				c++; }

			clearscreen ();
			drawline (c, 0, 1, SCREENH);
			blit ();
			sleep (33);

			} while (c < SCREENW-2);

		return; }

	static void
	init () {
		/* why would you want static arrays? go back to 1970 */
		name         = new String[MAXOFFENDER];
		licence      = new int   [MAXOFFENDER];
		fine         = new int   [MAXOFFENDER];
		points       = new int   [MAXOFFENDER];
		classOffence = new int   [MAXOFFENDER][SPEEDCLASSES];
		nOffender    = 0;

		for (int i = 0; i < MAXOFFENDER; ++i) {
			initoffender (i); }

		frameBuffer = new char[2][FBSZ + 1];
		boundBuffer = 0;

		input = new Scanner (System.in);

		demoMode = 0;
		demoIdx  = 0;

		/* get an even random number within the bounds of the quote array */
		quoteIdx = (int)(Math.random() * quote.length) & ~1;

		lastMsg = "";

		return; }

	static void
	drawmsg (int x, int y, int w, int h) {
		drawbox2 (x, y, w, h, "info");
		drawstring (x + 2, y + 2, lastMsg);
		return; }

	public static void
	main (String[] av) {
		/* would've been an enum if enums didn't fucking suck */
		final int ms_boot   = 0;
		final int ms_main   = 1;
		final int ms_add    = 2;
		final int ms_edit   = 3;
		final int ms_delete = 4;

		int state = ms_boot;
		state = ms_main;

		init ();
		for (;;) {
			clear   ();
			inspire (QUOTEX, QUOTEY);

			drawtotals    (TOTALSX, TOTALSY, TOTALSW, TOTALSH);
			drawoffenders (LISTX,   LISTY,   LISTW,   LISTH);
			drawmsg       (MSGX, MSGY, MSGW, MSGH);

			switch (state) {
				case ms_boot:
					bootanim ();
					state = ms_main;
					break;

				case ms_main:
					drawmainmenu ();
					blit         ();

					lastMsg = "";
					write (1, "option >> ");
					switch (getinput().charAt(0)) {
						case 'n': state = ms_add;    break;
						case 'd': state = ms_delete; break;
						case 'q': return;
						default : lastMsg = "invalid option"; break; }

					break;

				case ms_add:    if (add    () < 0) { state = ms_main; }  break;
				case ms_delete: if (delete () < 0) { state = ms_main; }  break; }}

		/* return; */ }

	static String
	getinput () {
		if (demoMode == 0) {
			/* if the user enters an empty string we'll crash trying to dereference
			 * it with charAt(). so we return a string containing a line feed to
			 * prevent crashes and indicate that we didn't get any input */
			String s = input.nextLine();
			if (s.length () <= 0) {
				s = "\n"; }

			return s; }

		String[] demo = {
			"n",
			"n", "pink",
			"l", "7",
			"o", "42",
			"q",
			"q" };

		return demo[demoIdx++]; }

	////////////////////////////////////////////////////////////////	
	///
	/// USEFUL THINGS (imagine; useful things)
	///

	static int
	abs (int i) {
		int mask = i >> 31;
		i = (i + mask) ^ mask;
		return i; }

	static int
	atoi (String a) {
		/* this implementation of atoi returns the absolute value(*)
		 * convenient since we don't want negative numbers here. but
		 * now we return a negative on error */

		/* skip leading whitespace */
		int len   = a.length();
		int start = 0;
		for (int i = 0; i < len; ++i) {
			char c = a.charAt(i);
			if (c > 0x20) {
				start = i;
				if (c == '-') {
					len--;
					start++; }

				break; }

			len--; }

		/* BUG: input of '-' will return 0 */
		if (len > 0) {
			char c = a.charAt(start);
			if (c < 0x30 || c > 0x39) {
				/* input doesnt begin with a number */
				return -1; }}

		int i = 0;
		while (len-- > 0) {
			char c = a.charAt(start++);
			if (c < 0x30 || c > 0x39) {
				break; }

			i *= 10;
			i += c - 0x30; }

		/* (*): atoi _does_ create an absolute value. but i forgot that since we
		 * return a signed value, since unsigned integers are the devils work,
		 * we could still return a negative value */
		return abs (i); }

	static void
	write (int fd, String s) {
		switch (fd) {
			case 1: System.out.print (s); break;
			case 2: System.err.print (s); break; }

		return; }

	static void
	sleep (int ms) {
		try { Thread.sleep (ms); } catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
		return; }

	static int
	lerp (int a, int b, float t) {
		return (int)((1.f - t) * a + t * b); }

	}
