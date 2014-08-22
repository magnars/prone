(function () {

  var lineNumbersWrapper = document.createElement('div');
  lineNumbersWrapper.className = 'code_linenums';

  Prism.hooks.add('after-highlight', function (env) {
    // works only for <code> wrapped inside <pre data-line-numbers> (not inline)
    var pre = env.element.parentNode;
    if (!pre || !/pre/i.test(pre.nodeName) || pre.className.indexOf('line-numbers') === -1) {
      return;
    }

    var linesNum = env.code.split('\n').length;

    var lines = new Array();
    for (var i = 1, l = linesNum + 1; i < l; i++) {
      lines.push(i);
    }

    lines = lines.join('</span><span>');

    lineNumbersWrapper.innerHTML = "<span>" + lines + "</span>";

    var highlightLine = pre.getAttribute("data-line");
    var highlightEl;
    if (highlightLine) {
      highlightEl = lineNumbersWrapper.childNodes[highlightLine - 1];
      if (highlightEl) {
        highlightEl.className = "highlight";
      }
    }

    pre.parentNode.insertBefore(lineNumbersWrapper, pre);

    var container = pre.parentNode;
    
    if (highlightEl && (container.offsetHeight - highlightEl.offsetTop < (highlightEl.offsetHeight * 3))) {
      container.scrollTop = highlightEl.offsetTop - (highlightEl.offsetHeight * 5)
    } else {
      container.scrollTop = 0;
    }
    
  });

}());