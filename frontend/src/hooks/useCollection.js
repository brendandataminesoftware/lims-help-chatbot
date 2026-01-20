import { useState, useEffect, useCallback } from 'react';
import { fetchCollectionMetadata } from '../api/chat';

const DEFAULT_LOGO = 'https://docs.dataminesoftware.com/Assets/Images/Datamine-Logo.png';

function getCollectionFromUrl() {
    const hash = window.location.hash.slice(1);
    return hash || null;
}

export function useCollection() {
    const [urlCollection, setUrlCollection] = useState(getCollectionFromUrl);
    const [resolvedCollection, setResolvedCollection] = useState(null);
    const [title, setTitle] = useState('Product Documentation');
    const [logo, setLogo] = useState(DEFAULT_LOGO);

    const loadMetadata = useCallback(async (collectionName) => {
        const metadata = await fetchCollectionMetadata(collectionName);
        setTitle(metadata.title);
        setLogo(metadata.logo);
        setResolvedCollection(metadata.resolvedCollection);

        // Update document title
        if (collectionName) {
            document.title = `Datamine Help - ${collectionName}`;
        } else {
            document.title = 'Datamine Help';
        }
    }, []);

    useEffect(() => {
        loadMetadata(urlCollection);
    }, [urlCollection, loadMetadata]);

    useEffect(() => {
        const handleHashChange = () => {
            setUrlCollection(getCollectionFromUrl());
        };

        window.addEventListener('hashchange', handleHashChange);
        return () => window.removeEventListener('hashchange', handleHashChange);
    }, []);

    return {
        urlCollection,
        resolvedCollection,
        title,
        logo
    };
}
