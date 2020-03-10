//
//  ViewController.swift
//  againbuttons
//
//  Created by Matt on 10/03/2020.
//  Copyright © 2020 Matt. All rights reserved.
//

/* 
   https://fluffy.es/intro-to-creating-ui-in-code-1/
   https://docs.swift.org/swift-book/LanguageGuide/Functions.html
   https://www.reddit.com/r/swift/comments/34hjqm/how_can_i_tell_which_button_was_pressed_if_i_am/
*/

import UIKit

class ViewController: UIViewController {

	let fx      = 32;
	let fy      = 128;
	let fwidth  = 4;
	let fheight = 4;
	let celldim = 64;

	var cell  = [UIButton]();
	var clear = [Int]();
	var mine  = [Int]();

	func numbertouching (x: Int, y: Int) -> Int {
		var n = 0;
		return n; }

	func doclear (x: Int, y: Int) {
		if (x < 0 || y < 0) {
			return; }

		if (x >= fwidth || y >= fheight) {
			return; }

		let idx = y*fwidth + x;
		if (clear[idx] != 0) {
			return; }

		if (mine[idx] != 0) {
			return; }

		clear[idx] = -1;

		doclear (x: x+1, y: y);
		doclear (x: x-1, y: y);
		doclear (x: x,   y: y+1);
		doclear (x: x,   y: y-1);

		return; }

	@IBAction func buttontap (_ sender: UIButton) {
		let idx = sender.tag;
		let row = idx / fwidth;
		let col = idx % fwidth;

		if (clear[idx] != 0) {
			return; }

		doclear (x: col, y: row);
		for i in 0 ..< fwidth*fheight {
			if (clear[i] != 0) {
				cell[i].backgroundColor = UIColor.gray; }}

		if (mine[idx] != 0) {
			cell[idx].backgroundColor = UIColor.red; }

		return; }

	func mkcell (x: Int, y: Int) -> UIButton {
		let b = UIButton ();
		b.frame = CGRect (x: x, y: y, width: celldim, height: celldim);
		b.backgroundColor = UIColor.black;
		return b; }

	override func viewDidLoad() {
		super.viewDidLoad()
		// Do any additional setup after loading the view, typically from a nib.

		var i = 0;
		for r in 0 ..< fheight {
			for c in 0 ..< fwidth {
				let b = mkcell (x: fx + c*(celldim+1), y: fy + r*(celldim+1));

				b.tag = i;
				b.addTarget (self, action: #selector (buttontap (_:)), for: .touchUpInside);

				self.view.addSubview (b);
				clear.append (0);
				cell.append  (b);
				mine.append  (0);

				i += 1; }}

		mine[ 3] = -1;
		mine[ 6] = -1;
		mine[ 9] = -1;
//		mine[12] = -1;

		return; }}
