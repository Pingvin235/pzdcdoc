/**
 * This file has to be loaded the last.
 */
const $$ = new function() {
	/**
	 * Marks a hash from URL in the left ToC and in document area.
	 */
	const markFragments = () => {
		const mark = () => {
			let hash = window.location.hash;
			if (hash) {
				hash = decodeURI(hash);
				markTocCurrent(hash);
				markContentCurrent(hash);
			}
		};

		mark();

		$(window).bind('hashchange', mark);
	}

	/**
	 * Marks current ToC menu item with 'current' class.
	 * @param {*} hash fragment ID starting from '#'.
	 */
	const markTocCurrent = (hash) => {
		const $selected = $('#toc.toc2 li > p > a.current');
		const $partLinks = $selected.closest('li').find('ul a');

		$partLinks.removeClass('current');
		$partLinks.filter('[href="' + hash + '"]').addClass('current');
	}

	/**
	 * Marks current content element with 'current' class.
	 * @param {*} hash fragment ID starting from '#'.
	 */
	const markContentCurrent = (hash) => {
		$('body > #content .current').removeClass('current');
		$('#content ' + hash).addClass('current');
	}

	/**
	 * Scrolls current item in left ToC to visible area.
	 */
	const scrollTocCurrentToVisible = () => {
		const toc2 = document.querySelector('#toc.toc2');
		const selected = toc2.querySelector('li a.current');
		toc2.scrollTop = selected.offsetTop - toc2.offsetTop;
	}

	/**
	 * Scrolls current content item to visible area.
	 */
	const scrollContentCurrentToVisible = () => {
		const html = document.querySelector('html');
		const current = html.querySelector('body > #content .current');
		if (current) {
			current.scrollIntoView();
			html.scrollTop -= 300;
		}
	}

	/**
	 * Calls the two scrolling functions above.
	 */
	const scrollCurrentToVisible = () => {
		scrollTocCurrentToVisible();
		scrollContentCurrentToVisible();
	}

	/**
	 * Inits search input.
	 */
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

			const searchValue = $input.val().toLowerCase();
			if (searchValue) {
				let tokens = '';

				searchValue.split(/\s+/).forEach(token => {
					if (!token.match(/^[\+\-\~]/))
						tokens += ' +';
					tokens += token + ' ';
				});

				const searchResult = idx.search(tokens).concat(substringSearch(searchValue));
				if (searchResult.length) {
					$searchCount.html('&nbsp;' + searchResult.length + '&nbsp;');
					searchResult.forEach(hit => {
						$tocLinks.each(function () {
							const $a = $(this);
							const url = $a.attr('href').replace(/\.\.\//g, '');
							if (url === hit.ref)
								$a.addClass('search').attr('target', '_blank');
						});
					});
				}
			}
		});
	}

	/**
	 * Checks if key event is enter.
	 * @param {*} e the event.
	 * @returns
	 */
	 const enterPressed = (e) => {
		return (e.keyCode || e.which) == 13;
	}

	/**
	 * Builds full-text search index on a first usage.
	 * @returns
	 */
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

	/**
	 * Searches substring in documents.
	 * @param {*} value searched substring.
	 * @returns array of found documents.
	 */
	const substringSearch = (value) => {
		const result = [];

		$$.documents.forEach(doc => {
			if (doc.content.includes(value) || doc.title.toLowerCase().includes(value))
				result.push(doc);
		})

		return result;
	}

	// public functions
	this.markFragments = markFragments;
	this.scrollCurrentToVisible = scrollCurrentToVisible;
	this.initSearch = initSearch;
}

$(function () {
	$$.markFragments();
	setTimeout(() => $$.scrollCurrentToVisible(), 1000);
})
