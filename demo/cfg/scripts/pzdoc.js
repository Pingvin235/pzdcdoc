/**
 * This file has to be loaded the last.
 */
const $$ = new function() {
	const scrollTocToVisible = () => {
		const toc2 = document.querySelector('#toc.toc2');
		const selected = toc2.querySelector('li > p > a.current');
		toc2.scrollTop = selected.offsetTop - toc2.offsetTop;
	};
	
	// public functions
	this.scrollTocToVisible = scrollTocToVisible;
}


$(function () {
	$$.scrollTocToVisible();
});