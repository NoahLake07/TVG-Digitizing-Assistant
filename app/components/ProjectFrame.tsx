import React, { useEffect } from 'react';

function ProjectFrame(props) {
    useEffect(() => {
        function handleKeyDown(e: KeyboardEvent) {
            // Ignore if focus is in an input, textarea, or contenteditable
            const tag = (e.target as HTMLElement).tagName;
            const isEditable = (e.target as HTMLElement).isContentEditable;
            if (tag === 'INPUT' || tag === 'TEXTAREA' || isEditable) return;

            if (e.key === '[') {
                // Call your previous conversion function
                props.onPreviousConversion?.();
                e.preventDefault();
            } else if (e.key === ']') {
                // Call your next conversion function
                props.onNextConversion?.();
                e.preventDefault();
            }
        }

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [props.onPreviousConversion, props.onNextConversion]);

    // ... existing code ...
}

export default ProjectFrame; 