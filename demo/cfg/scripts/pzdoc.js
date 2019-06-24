/**
 * This file has to be loaded the last.
 */
const $$ = new function() {
	const scrollTocToVisible = () => {
		const toc2 = document.querySelector('#toc.toc2');
		const selected = $(toc2).find('li a.current').last()[0];
		toc2.scrollTop = selected.offsetTop - toc2.offsetTop;
	};
	
	const markPart = () => {
		const mark = function () {
			const hash = location.hash;
			if (hash) {
				const $selected = $('#toc.toc2 li > p > a.current');
				const $partLinks = $selected.closest('li').find('ul a');
				
				$partLinks.removeClass('current');
				$partLinks.filter('[href="' + hash + '"]').addClass('current');
			}
		};
		
		mark();
		$(window).bind('hashchange', mark);
	};
	
	// public functions
	this.scrollTocToVisible = scrollTocToVisible;
	this.markPart = markPart;
}


$(function () {
	$$.markPart();
	$$.scrollTocToVisible();
});