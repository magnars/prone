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

    var start = 1;

    if (pre.hasAttribute('data-start')) {
      start = parseInt(pre.getAttribute('data-start'), 10) - 1;
    }

    var lines = new Array();
    for (var i = start, l = linesNum + start; i < l; i++) {
      lines.push(i);
    }

    lines = lines.join('</span><span>');

    lineNumbersWrapper.innerHTML = "<span>" + lines + "</span>";

    var highlight = pre.getAttribute("data-line");
    if (highlight) {
      var child = lineNumbersWrapper.childNodes[highlight - 1];
      if (child) {
        child.className = "highlight";
      }
    }

    pre.parentNode.insertBefore(lineNumbersWrapper, pre);

  });

}());