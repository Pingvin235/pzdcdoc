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

	const buildIndex = () => {
		return lunr(function () {
			// https://lunrjs.com/guides/language_support.html
			this.use(lunr.multiLanguage('en', 'ru', 'de'));

			this.ref('ref')
			this.field('title')
			this.field('content')
		
			$$.documents.forEach(function (doc) {
				this.add(doc)
			}, this)
		});
	}

	const initSearch = () => {
		let idx = undefined;

		const $input = $('#search input');
		$input.on("keypress", (e) => {
			if (!idx)
				idx = buildIndex();

			if (!enterPressed(e)) return;

			const $tocLinks = $('#toc.toc2 li a');
			$tocLinks.removeClass('search').removeAttr('target');

			const $searchCount = $('#search-count');
			$searchCount.text('');

			let searchValue = $input.val();
			if (searchValue) {
				const tokens = searchValue.split(/\s+/);
				
				searchValue = '';
				tokens.forEach(token => {
					if (!token.match(/^[\+\-\~]/))
						searchValue += ' +';
					searchValue += token + ' ';
				});

				const searchResult = idx.search(searchValue);
				$searchCount.text(searchResult.length);
				searchResult.forEach(hit => {
					$tocLinks.each(function () {
						const $a = $(this);
						const url = $a.attr('href').replace('../', '');
						if (url === hit.ref)
							$a.addClass('search').attr('target', '_blank');
					});
				});
			}
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
});
