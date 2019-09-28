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
			let hash = location.hash;
			if (hash) {
				hash = decodeURI(hash);
				const $selected = $('#toc.toc2 li > p > a.current');
				const $partLinks = $selected.closest('li').find('ul a');
				
				$partLinks.removeClass('current');
				$partLinks.filter('[href="' + hash + '"]').addClass('current');
			}
		};
		
		mark();
		$(window).bind('hashchange', mark);
	};

	const enterPressed = ($e) => {
		return ($e.keyCode || $e.which) == 13;
	};

	const initSearch = () => {
		const idx = lunr(function () {
			// https://lunrjs.com/guides/language_support.html
			this.use(lunr.multiLanguage('en', 'ru', 'de'));

			this.ref('url')
			this.field('title')
			this.field('content')
		
			$$.documents.forEach(function (doc) {
				this.add(doc)
			}, this)
		});

		const $input = $('#search input');
		$input.on("keypress", (e) => {
			if (!enterPressed(e)) return;

			const search = idx.search($input.val());
			console.log(search);
		});
	};
	
	// public functions
	this.scrollTocToVisible = scrollTocToVisible;
	this.markPart = markPart;
	this.initSearch = initSearch;
}

$(function () {
	$$.markPart();
	$$.scrollTocToVisible();
	$$.initSearch();
});